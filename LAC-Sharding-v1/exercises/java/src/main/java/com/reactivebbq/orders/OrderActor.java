package com.reactivebbq.orders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class OrderActor {
    interface Command extends SerializableMessage {}
    interface Event extends SerializableMessage {}

    static class OpenOrder implements Command {
        private final Server server;
        private final Table table;

        @JsonCreator
        public OpenOrder(@JsonProperty("server") Server server, @JsonProperty("table") Table table) {
            this.server = server;
            this.table = table;
        }

        public Server getServer() {
            return server;
        }

        public Table getTable() {
            return table;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OpenOrder openOrder = (OpenOrder) o;
            return Objects.equals(server, openOrder.server) &&
                    Objects.equals(table, openOrder.table);
        }

        @Override
        public int hashCode() {
            return Objects.hash(server, table);
        }
    }

    static class OrderOpened implements Event {
        private final Order order;

        @JsonCreator
        public OrderOpened(@JsonProperty Order order) {
            this.order = order;
        }

        public Order getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderOpened that = (OrderOpened) o;
            return Objects.equals(order, that.order);
        }

        @Override
        public int hashCode() {
            return Objects.hash(order);
        }
    }

    static class AddItemToOrder implements Command {
        private final OrderItem item;

        @JsonCreator
        public AddItemToOrder(@JsonProperty("item") OrderItem item) {
            this.item = item;
        }

        public OrderItem getItem() {
            return item;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddItemToOrder that = (AddItemToOrder) o;
            return Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    static class ItemAddedToOrder implements Event {
        private final Order order;

        @JsonCreator
        public ItemAddedToOrder(@JsonProperty Order order) {
            this.order = order;
        }

        public Order getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemAddedToOrder that = (ItemAddedToOrder) o;
            return Objects.equals(order, that.order);
        }

        @Override
        public int hashCode() {
            return Objects.hash(order);
        }
    }

    static class GetOrder implements Command {}

    static class OrderNotFoundException extends IllegalStateException {
        private final OrderId orderId;

        public OrderNotFoundException(OrderId orderId) {
            super("Order Not Found: "+orderId);
            this.orderId = orderId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderNotFoundException that = (OrderNotFoundException) o;
            return Objects.equals(orderId, that.orderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId);
        }
    }

    static class DuplicateOrderException extends IllegalStateException {
        private final OrderId orderId;

        public DuplicateOrderException(OrderId orderId) {
            super("Duplicate Order: "+orderId);
            this.orderId = orderId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DuplicateOrderException that = (DuplicateOrderException) o;
            return Objects.equals(orderId, that.orderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId);
        }
    }
}
