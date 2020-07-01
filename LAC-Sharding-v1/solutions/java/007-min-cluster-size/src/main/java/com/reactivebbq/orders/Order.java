package com.reactivebbq.orders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Vector;

public class Order implements SerializableMessage {
    private final OrderId id;
    private final Server server;
    private final Table table;
    private final Vector<OrderItem> items;

    @JsonCreator
    public Order(@JsonProperty("id") OrderId id, @JsonProperty("server") Server server, @JsonProperty("table") Table table, @JsonProperty("items") Vector<OrderItem> items) {
        this.id = id;
        this.server = server;
        this.table = table;
        this.items = new Vector<>(items);
    }

    public OrderId getId() {
        return id;
    }

    public Server getServer() {
        return server;
    }

    public Table getTable() {
        return table;
    }

    public Vector<OrderItem> getItems() {
        return new Vector<>(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) &&
                Objects.equals(server, order.server) &&
                Objects.equals(table, order.table) &&
                Objects.equals(items, order.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, server, table, items);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", server=" + server +
                ", table=" + table +
                ", items=" + items +
                '}';
    }

    public Order withItem(OrderItem item) {
        Vector<OrderItem> updatedItems = new Vector<>(items);
        updatedItems.add(item);

        return new Order(id, server, table, updatedItems);
    }
}
