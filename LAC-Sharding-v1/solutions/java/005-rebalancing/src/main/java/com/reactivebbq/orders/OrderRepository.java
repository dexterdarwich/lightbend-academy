package com.reactivebbq.orders;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OrderRepository {
    CompletableFuture<Order> update(Order order);
    CompletableFuture<Optional<Order>> find(OrderId orderId);
}
