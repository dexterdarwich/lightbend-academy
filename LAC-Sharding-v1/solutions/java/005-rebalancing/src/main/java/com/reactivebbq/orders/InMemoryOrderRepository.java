package com.reactivebbq.orders;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class InMemoryOrderRepository implements OrderRepository {
    private final HashMap<OrderId, Order> orders = new HashMap<>();
    private final Executor executor;

    public InMemoryOrderRepository(Executor executor) {
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Order> update(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                orders.put(order.getId(), order);
            }
            return order;
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Order>> find(OrderId orderId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                return Optional.ofNullable(orders.get(orderId));
            }
        });
    }
}
