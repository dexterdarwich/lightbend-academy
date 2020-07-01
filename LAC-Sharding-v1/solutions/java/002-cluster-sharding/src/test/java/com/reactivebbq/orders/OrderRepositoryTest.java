package com.reactivebbq.orders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static com.reactivebbq.orders.OrderHelpers.*;

abstract class OrderRepositoryTest {
    public abstract OrderRepository createOrderRepository();
    public abstract void destroyOrderRepository();

    private OrderRepository orderRepository;

    @BeforeEach
    public void setup() {
        orderRepository = createOrderRepository();
    }

    @AfterEach
    public void teardown() {
        destroyOrderRepository();
    }

    @Test
    public void find_shouldReturnEmptyIfTheRepositoryIsEmpty() {
        OrderId orderId = generateOrderId();
        Optional<Order> result = orderRepository.find(orderId).join();

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void find_shouldReturnEmptyIfTheRepositoryIsNotEmptyButTheOrderDoesntExist() {
        for (int i = 0; i < 10; i++) {
            orderRepository.update(generateOrder()).join();
        }

        OrderId orderId = generateOrderId();
        Optional<Order> result = orderRepository.find(orderId).join();

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void find_shouldReturnTheOrderIfItIsTheOnlyOne() {
        Order order = generateOrder();

        Order updateResult = orderRepository.update(order).join();
        Optional<Order> findResult = orderRepository.find(order.getId()).join();

        assertEquals(order, updateResult);
        assertTrue(findResult.isPresent());
        assertEquals(order, findResult.get());
    }

    @Test
    public void find_shouldReturnTheCorrectOrderWhenThereIsMoreThanOne() {
        for (int i = 0; i < 10; i++) {
            orderRepository.update(generateOrder()).join();
        }

        Order order = generateOrder();

        Order updateResult = orderRepository.update(order).join();
        Optional<Order> findResult = orderRepository.find(order.getId()).join();

        assertEquals(order, updateResult);
        assertTrue(findResult.isPresent());
        assertEquals(order, findResult.get());
    }

    @Test
    public void update_shouldAddTheOrderToTheRepoIfItDoesntExist() {
        Order order = generateOrder();

        Order updateResult = orderRepository.update(order).join();
        Optional<Order> findResult = orderRepository.find(order.getId()).join();

        assertEquals(order, updateResult);
        assertTrue(findResult.isPresent());
        assertEquals(order, findResult.get());
    }

    @Test
    public void update_shouldOverwriteTheOrderIfItExists() {
        Order order = generateOrder();
        Order updated = order.withItem(generateOrderItem());

        Order insertResult = orderRepository.update(order).join();
        Order updateResult = orderRepository.update(updated).join();
        Optional<Order> findResult = orderRepository.find(order.getId()).join();

        assertEquals(order, insertResult);
        assertEquals(updated, updateResult);
        assertTrue(findResult.isPresent());
        assertEquals(updated, findResult.get());
    }

}
