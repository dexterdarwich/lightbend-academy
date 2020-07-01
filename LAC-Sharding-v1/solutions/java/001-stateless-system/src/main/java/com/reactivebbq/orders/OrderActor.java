package com.reactivebbq.orders;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.Patterns.pipe;

public class OrderActor extends AbstractActor {
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

    static class Envelope implements SerializableMessage {
        private final OrderId orderId;
        private final Command command;

        public Envelope(OrderId orderId, Command command) {
            this.orderId = orderId;
            this.command = command;
        }

        public OrderId getOrderId() {
            return orderId;
        }

        public Command getCommand() {
            return command;
        }
    }

    static Props props(OrderRepository repository) {
        return Props.create(
            OrderActor.class,
            () -> new OrderActor(repository)
        );
    }

    private final OrderRepository repository;
    private final LoggingAdapter log;

    public OrderActor(OrderRepository repository) {
        this.repository = repository;
        this.log = Logging.getLogger(getContext().getSystem(), this);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(
                Envelope.class,
                envelope -> envelope.getCommand() instanceof OpenOrder,
                envelope -> {
                    OrderId orderId = envelope.getOrderId();
                    OpenOrder cmd = (OpenOrder) envelope.getCommand();

                    log.info("["+orderId+"] OpenOrder("+cmd.getServer()+","+cmd.getTable()+")");

                    pipe(repository.find(orderId).thenCompose(option ->
                        option
                            .map((order) -> this.<OrderOpened>duplicateOrder(orderId))
                            .orElseGet(() -> openOrder(orderId, cmd.getServer(), cmd.getTable()))
                    ), getContext().getDispatcher()).to(sender());
                }
            )
            .match(
                Envelope.class,
                envelope -> envelope.getCommand() instanceof AddItemToOrder,
                envelope -> {
                    OrderId orderId = envelope.getOrderId();
                    AddItemToOrder cmd = (AddItemToOrder) envelope.getCommand();

                    log.info("["+orderId+"] AddItemToOrder("+cmd.getItem()+")");

                    pipe(repository.find(orderId).thenCompose(option ->
                        option
                            .map((order) -> addItem(order, cmd.getItem()))
                            .orElseGet(() -> orderNotFound(orderId))
                    ), getContext().getDispatcher()).to(sender());
                }
            )
            .match(
                Envelope.class,
                envelope -> envelope.getCommand() instanceof GetOrder,
                envelope -> {
                    OrderId orderId = envelope.getOrderId();

                    log.info("["+orderId+"] GetOrder()");

                    pipe(repository.find(orderId).thenCompose(option ->
                        option
                            .map(CompletableFuture::completedFuture)
                            .orElseGet(() -> orderNotFound(orderId))
                    ), getContext().getDispatcher()).to(sender());
                }
            )
            .build();
    }

    private CompletableFuture<OrderOpened> openOrder(OrderId orderId, Server server, Table table) {
        return repository.update(new Order(orderId, server, table, new Vector<>()))
            .thenApply(OrderOpened::new);
    }

    private <T> CompletableFuture<T> duplicateOrder(OrderId orderId) {
        return CompletableFuture.failedFuture(new DuplicateOrderException(orderId));
    }

    private CompletableFuture<ItemAddedToOrder> addItem(Order order, OrderItem item) {
        return repository.update(order.withItem(item))
            .thenApply(ItemAddedToOrder::new);
    }

    private <T> CompletableFuture<T> orderNotFound(OrderId orderId) {
        return CompletableFuture.failedFuture(new OrderNotFoundException(orderId));
    }
}
