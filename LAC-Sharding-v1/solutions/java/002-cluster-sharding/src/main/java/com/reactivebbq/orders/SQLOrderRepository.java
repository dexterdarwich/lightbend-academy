package com.reactivebbq.orders;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLOrderRepository implements OrderRepository {

    private EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("reactivebbq.Orders");
    private ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<>();
    private Executor executor;

    public SQLOrderRepository(Executor executor) {
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Order> update(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            OrderDBO dbo = (new OrderDBO()).apply(order);
            transaction(em -> em.merge(dbo));
            return order;
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Order>> find(OrderId orderId) {
        return CompletableFuture.supplyAsync(() -> transaction(em -> Optional.ofNullable(
            em.find(OrderDBO.class, orderId.getValue())
        ).map(this::dboToOrder)), executor);
    }

    private Order dboToOrder(OrderDBO dbo) {
        OrderId orderId = new OrderId(dbo.getId());
        Server server = new Server(dbo.getServerName());
        Table table = new Table(dbo.getTableNumber());
        Vector<OrderItem> items = new Vector<>();

        for(OrderItemDBO item : dbo.getItems()) {
            items.add(new OrderItem(item.getName(), item.getSpecialInstructions()));
        }

        return new Order(orderId, server, table, items);
    }

    private EntityManager getEntityManager() {
        if(threadLocalEntityManager.get() == null) {
            threadLocalEntityManager.set(entityManagerFactory.createEntityManager());
        }

        return threadLocalEntityManager.get();
    }

    private <T> T transaction(Function<EntityManager, T> f) {
        EntityManager entityManager = getEntityManager();

        entityManager.getTransaction().begin();
        T result = f.apply(entityManager);
        entityManager.getTransaction().commit();
        return result;
    }
}

@Embeddable
class OrderItemDBO {
    private String name = "";
    private String specialInstructions = "";

    public OrderItemDBO apply(OrderItem orderItem) {
        name = orderItem.getName();
        specialInstructions = orderItem.getSpecialInstructions();

        return this;
    }

    public String getName() {
        return name;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }
}

@Entity
@javax.persistence.Table(name = "orders")
class OrderDBO {

    @Id
    private UUID id;

    private String serverName;
    private int tableNumber;

    @ElementCollection(targetClass = OrderItemDBO.class)
    private List<OrderItemDBO> items;

    OrderDBO apply(Order order) {
        id = order.getId().getValue();
        serverName = order.getServer().getName();
        tableNumber = order.getTable().getNumber();
        items = order
                .getItems()
                .stream()
                .map(item -> (new OrderItemDBO()).apply(item))
                .collect(Collectors.toList());

        return this;
    }

    public UUID getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public List<OrderItemDBO> getItems() {
        return items;
    }

}
