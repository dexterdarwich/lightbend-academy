package com.reactivebbq.orders;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLOrderRepositoryTest extends OrderRepositoryTest {
    private Executor executor;

    @Override
    public OrderRepository createOrderRepository() {
        executor = Executors.newFixedThreadPool(100);
        return new SQLOrderRepository(executor);
    }

    @Override
    public void destroyOrderRepository() {
        ((ExecutorService) executor).shutdown();
    }
}
