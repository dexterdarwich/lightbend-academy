package com.reactivebbq.orders;

import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static com.reactivebbq.orders.OrderHelpers.*;

class OrderTest {

    @Test
    public void equals_shouldReturnTrue_forTheSameOrder() {
        OrderId id = new OrderId();
        Server server = new Server("Jane");
        Table table = new Table(5);

        Vector<OrderItem> items1 = new Vector<>();
        items1.add(new OrderItem("Steak", "Medium"));
        items1.add(new OrderItem("Beyond Steak", "Well Done"));

        Vector<OrderItem> items2 = new Vector<>();
        items2.add(new OrderItem("Steak", "Medium"));
        items2.add(new OrderItem("Beyond Steak", "Well Done"));

        Order o1 = new Order(id, server, table, items1);
        Order o2 = new Order(id, server, table, items2);

        assertEquals(o1, o2);
    }

    @Test
    public void equals_shouldReturnTrue_forADifferentOrder() {
        OrderId id1 = new OrderId();
        OrderId id2 = new OrderId();
        Server server = new Server("Jane");
        Table table = new Table(5);

        Vector<OrderItem> items1 = new Vector<>();
        items1.add(new OrderItem("Steak", "Medium"));
        items1.add(new OrderItem("Beyond Steak", "Well Done"));

        Vector<OrderItem> items2 = new Vector<>();
        items2.add(new OrderItem("Steak", "Medium"));

        Order o1 = new Order(id1, server, table, items1);
        Order o2 = new Order(id1, server, table, items2);
        Order o3 = new Order(id2, server, table, items1);

        assertNotEquals(o1, o2);
        assertNotEquals(o2, o3);
        assertNotEquals(o1, o3);
    }

    @Test
    public void withItem_shouldReturnACopyOfTheOrderWithTheNewItem() {
        OrderItem item = generateOrderItem();

        Order order = generateOrder(new Vector<>());
        Order updated = order.withItem(item);

        Vector<OrderItem> expected = new Vector<>();
        expected.add(item);

        assertEquals(new Vector<>(), order.getItems());
        assertEquals(expected, updated.getItems());
    }

    @Test
    public void withItem_shouldReturnACopyOfTheOrderWithTheNewItemAppendedToExisting() {
        Vector<OrderItem> oldItems = generateOrderItems(10);
        OrderItem item = generateOrderItem();

        Order order = generateOrder(oldItems);
        Order updated = order.withItem(item);

        Vector<OrderItem> expected = new Vector<>(oldItems);
        expected.add(item);

        assertEquals(oldItems, order.getItems());
        assertEquals(expected, updated.getItems());
    }

}
