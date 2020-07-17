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

        String expectedShardId = String.valueOf(Math.abs(orderId.hashCode() % maxShards));

        String shardId = OrderActor.messageExtractor(maxShards).shardId(envelope);

        assertEquals(expectedShardId, shardId);
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
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
        assertEquals(new OrderActor.DuplicateOrderException(orderId), ex);
    }

    @Test
    public void openOrder_shouldReturnTheRepositoryFailureIfTheRepositoryFails() {
        Server server = generateServer();
        Table table = generateTable();

        RuntimeException expectedException = new RuntimeException("Repository Failure");
        repo.mockUpdate((ignore) -> CompletableFuture.failedFuture(expectedException));

        sender.send(orderActor, new OrderActor.OpenOrder(server, table));

        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
        assertEquals(expectedException, ex);
    }

    @Test
    public void addItemToOrder_shouldReturnAnOrderNotFoundExceptionIfTheOrderHasntBeenOpened() {
        OrderItem item = generateOrderItem();

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
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
    public void addItemToOrder_shouldReturnTheRepositoryFailureIfTheRepositoryFails() {
        Order order = openOrder();

        OrderItem item = generateOrderItem();

        Exception expectedException = new RuntimeException("Repository Failure");
        repo.mockUpdate(ignore -> CompletableFuture.failedFuture(expectedException));

        sender.send(orderActor, new OrderActor.AddItemToOrder(item));
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
        assertEquals(expectedException, ex);
    }

    @Test
    public void getOrder_shouldReturnAnOrderNotFoundExceptionIfTheOrderHasntBeenOpened() {
        sender.send(orderActor, new OrderActor.GetOrder());
        Throwable ex = sender.expectMsgClass(Status.Failure.class).cause().getCause();
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
