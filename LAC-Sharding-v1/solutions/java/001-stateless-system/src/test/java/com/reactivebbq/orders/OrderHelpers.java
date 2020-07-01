package com.reactivebbq.orders;

import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Vector;

public class OrderHelpers {
    private static Random rnd = new Random(System.currentTimeMillis());
    private static PrimitiveIterator.OfInt rndInts = rnd.ints(10000, 0, 1000000).distinct().iterator();

    public static OrderId generateOrderId() {
        return new OrderId();
    }

    public static Server generateServer() {
        return new Server("ServerName"+rndInts.nextInt());
    }

    public static Table generateTable() {
        return new Table(rndInts.nextInt());
    }

    public static OrderItem generateOrderItem() {
        return new OrderItem("ItemName" + rndInts.nextInt(), "SpecialInstructions" + rndInts.nextInt());
    }

    public static Vector<OrderItem> generateOrderItems(int quantity) {
        Vector<OrderItem> items = new Vector<>();
        for(int i = 0; i < quantity; i++) {
            items.add(generateOrderItem());
        }
        return items;
    }

    public static Vector<OrderItem> generateOrderItems() {
        return generateOrderItems(10);
    }

    public static Order generateOrder() {
        return new Order(generateOrderId(), generateServer(), generateTable(), generateOrderItems());
    }

    public static Order generateOrder(Vector<OrderItem> items) {
        return new Order(generateOrderId(), generateServer(), generateTable(), items);
    }

}
