package com.reactivebbq.orders;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.cluster.sharding.ShardRegion;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static com.reactivebbq.orders.OrderHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderActorTest extends AkkaTest {

    static class MockRepo extends InMemoryOrderRepository {
        private Queue<Function<Order, CompletableFuture<Order>>> updates = new LinkedList<>();
        private Queue<Function<OrderId, CompletableFuture<Optional<Order>>>> finds = new LinkedList<>();

        public MockRepo(Executor executor) {
            super(executor);
        }

        @Override
        public CompletableFuture<Order> update(Order order) {
            if(!updates.isEmpty()) {
                return updates.poll().apply(order);
            } else {
                return super.update(order);
            }
        }

        @Override
        public CompletableFuture<Optional<Order>> find(OrderId orderId) {
            if(!finds.isEmpty()) {
                return finds.poll().apply(orderId);
            } else {
                return super.find(orderId);
            }
        }

        public MockRepo mockUpdate(Function<Order, CompletableFuture<Order>> u) {
            updates.offer(u);
            return this;
        }

        public MockRepo mockFind(Function<OrderId, CompletableFuture<Optional<Order>>> f) {
            finds.offer(f);
            return this;
        }
    }

    private MockRepo repo;
    private OrderId orderId;
    private TestKit sender;
    private TestKit parent;
    private ActorRef orderActor;

    private Order openOrder() {
        Server server = generateServer();
        Table table = generateTable();

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));

        return sender.expectMsgClass(OrderActor.OrderOpened.class).getOrder();
    }

    @BeforeEach
    public void setup() {
        repo = new MockRepo(system.getDispatcher());
        orderId = generateOrderId();
        sender = new TestKit(system);
        parent = new TestKit(system);
        orderActor = parent.childActorOf(
            OrderActor.props(repo),
            orderId.getValue().toString()
        );
    }

    @Test
    public void idExtractor_shouldReturnTheExpectedId() {
        int maxShards = system.settings().config().getInt("orders.max-shards");

        OrderId orderId = generateOrderId();
        OrderActor.GetOrder message = new OrderActor.GetOrder();
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, message);

        String stringId = OrderActor.messageExtractor(maxShards).entityId(envelope);

        assertEquals(orderId.getValue().toString(), stringId);
    }

    @Test
    public void messageExtractor_shouldReturnTheExpectedMessage() {
        int maxShards = system.settings().config().getInt("orders.max-shards");

        OrderId orderId = generateOrderId();
        OrderActor.GetOrder message = new OrderActor.GetOrder();
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, message);

        Object msgObject = OrderActor.messageExtractor(maxShards).entityMessage(envelope);

        assertEquals(message, msgObject);
    }

    @Test
    public void shardExtractor_shouldReturnTheExpectedshardId() {
        int maxShards = system.settings().config().getInt("orders.max-shards");

        OrderId orderId = generateOrderId();
        OrderActor.GetOrder message = new OrderActor.GetOrder();
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, message);
        ShardRegion.StartEntity startEntity = new ShardRegion.StartEntity(orderId.getValue().toString());

        String expectedShardId = String.valueOf(Math.abs(orderId.hashCode() % 30));

        String shardId = OrderActor.messageExtractor(maxShards).shardId(envelope);
        String startEntityShardId = OrderActor.messageExtractor(maxShards).shardId(startEntity);

        assertEquals(expectedShardId, shardId);
        assertEquals(expectedShardId, startEntityShardId);
    }

    @Test
    public void theActor_shouldLoadItsStateFromTheRepositoryWhenCreated() {
        Order order = generateOrder();
        repo.update(order).join();

        ActorRef actor = parent
            .childActorOf(
                OrderActor.props(repo),
                order.getId().getValue().toString());

        sender.send(actor, new OrderActor.GetOrder());
        sender.expectMsg(order);
    }

    @Test
    public void theActor_shouldTerminateWhenItFailsToLoadFromTheRepo() {
        Order order = generateOrder();

        MockRepo mockRepo = new MockRepo(system.getDispatcher());
        mockRepo.mockFind(ignore -> CompletableFuture.failedFuture(new Exception("Repo Failed")));

        ActorRef actor = parent
            .childActorOf(
                OrderActor.props(mockRepo),
                order.getId().getValue().toString());

        parent.watch(actor);
        parent.expectTerminated(actor);
    }

    @Test
    public void openOrder_shouldInitializeTheOrder() {
        Server server = generateServer();
        Table table = generateTable();

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));
        Order order = sender.expectMsgClass(OrderActor.OrderOpened.class).getOrder();

        assertEquals(Optional.of(order), repo.find(order.getId()).join());

        assertEquals(server, order.getServer());
        assertEquals(table, order.getTable());
    }

    @Test
    public void openOrder_shouldReturnAnErrorIfTheOrderIsAlreadyOpen() {
        Server server = generateServer();
        Table table = generateTable();

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));
        sender.expectMsgClass(OrderActor.OrderOpened.class);

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause();
        assertEquals(new OrderActor.DuplicateOrderException(orderId), ex);
    }

    @Test
    public void openOrder_shouldReturnTheRepositoryFailureIfTheRepositoryFailsAndTerminate() {
        Server server = generateServer();
        Table table = generateTable();

        parent.watch(orderActor);

        RuntimeException expectedException = new RuntimeException("Repository Failure");
        repo.mockUpdate((ignore) -> CompletableFuture.failedFuture(expectedException));

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));

        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
        assertEquals(expectedException, ex);

        parent.expectTerminated(orderActor);
    }

    @Test
    public void openOrder_shouldNotAllowFurtherInteractionsWhileAnUpdateIsInProgress() {
        Order order = generateOrder(orderId, new Vector<>());

        repo.mockUpdate(o -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
                return o;
            } catch (InterruptedException ex) {
                throw new CompletionException(ex);
            }
        }));

        sender.send(orderActor, new OrderActor.OpenOrder(order.getServer(), order.getTable()));
        sender.send(orderActor, new OrderActor.OpenOrder(order.getServer(), order.getTable()));

        sender.expectMsg(new OrderActor.OrderOpened(order));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause();
        assertEquals(new OrderActor.DuplicateOrderException(orderId), ex);
    }

    @Test
    public void addItemToOrder_shouldReturnAnOrderNotFoundExceptionIfTheOrderHasntBeenOpened() {
        OrderItem item = generateOrderItem();

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause();
        assertEquals(new OrderActor.OrderNotFoundException(orderId), ex);
    }

    @Test
    public void addItemToOrder_shouldAddTheItemToTheOrder() {
        Order order = openOrder();

        OrderItem item = generateOrderItem();

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        sender.expectMsg(new OrderActor.ItemAddedToOrder(order.withItem(item)));
    }

    @Test
    public void addItemToOrder_shouldAddMultipleItemsToTheOrder() {
        Order order = openOrder();

        Vector<OrderItem> items = generateOrderItems(10);

        for(OrderItem item : items) {
            order = order.withItem(item);

            sender.send(orderActor, new OrderActor.AddItemToOrder(item));
            sender.expectMsg(new OrderActor.ItemAddedToOrder(order));
        }
    }

    @Test
    public void addItemToOrder_shouldReturnTheRepositoryFailureIfTheRepositoryFailsAndTerminate() {
        Order order = openOrder();

        parent.watch(orderActor);

        OrderItem item = generateOrderItem();

        Exception expectedException = new RuntimeException("Repository Failure");
        repo.mockUpdate(ignore -> CompletableFuture.failedFuture(expectedException));

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
        assertEquals(expectedException, ex);

        parent.expectTerminated(orderActor);
    }

    @Test
    public void addItemToOrder_shouldNotAllowFurtherInteractionsWhileAnUpdateIsInProgress() {
        Order order = openOrder();

        OrderItem item1 = generateOrderItem();
        Order updated1 = order.withItem(item1);

        OrderItem item2 = generateOrderItem();
        Order updated2 = updated1.withItem(item2);

        repo.mockUpdate(o -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
                return o;
            } catch (InterruptedException ex) {
                throw new CompletionException(ex);
            }
        }));

        sender.send(orderActor, new OrderActor.AddItemToOrder(item1));
        sender.send(orderActor, new OrderActor.AddItemToOrder(item2));

        sender.expectMsg(new OrderActor.ItemAddedToOrder(updated1));
        sender.expectMsg(new OrderActor.ItemAddedToOrder(updated2));
    }

    @Test
    public void getOrder_shouldReturnAnOrderNotFoundExceptionIfTheOrderHasntBeenOpened() {
        sender.send(orderActor, new OrderActor.GetOrder());
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause();
        assertEquals(new OrderActor.OrderNotFoundException(orderId), ex);
    }

    @Test
    public void getOrder_shouldReturnAnOpenOrder() {
        Order order = openOrder();

        sender.send(orderActor, new OrderActor.GetOrder());
        sender.expectMsg(order);
    }

    @Test
    public void getOrder_shouldReturnAnUpdatedOrder() {
        Order order = openOrder();
        OrderItem item = generateOrderItem();

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        sender.expectMsgClass(OrderActor.ItemAddedToOrder.class);

        sender.send(orderActor, new OrderActor.GetOrder());
        sender.expectMsg(order.withItem(item));
    }

}
