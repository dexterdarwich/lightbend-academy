package com.reactivebbq.orders;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.Patterns.pipe;

public class OrderActor extends AbstractActor {
    private final OrderRepository repository;
    private final LoggingAdapter log;
    private final OrderId orderId;

    static Props props(OrderRepository repository) {
        return Props.create(OrderActor.class, repository);
    }

    public OrderActor(OrderRepository repository) {
        log = Logging.getLogger(getContext().getSystem(), this);
        this.repository = repository;
        orderId = OrderId.fromString(getSelf().path().name());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(OpenOrder.class, openOrder -> {
                            Server server = openOrder.getServer();
                            Table table = openOrder.getTable();
                            log.info("[" + orderId + "] OpenOrder(" + server + ", " + table + ")");
                            CompletableFuture<Optional<Order>> future = repository.find(orderId);
                            pipe(future.thenCompose(option ->
                                            option
                                                    .map(order -> this.<OrderOpened>duplicateOrder(orderId))
                                                    .orElseGet(() -> openOrder(orderId, server, table))
                                    ),
                                    getContext().getDispatcher()).to(getSender());
                        })
                .match(AddItemToOrder.class, addItemToOrder -> {
                            OrderItem item = addItemToOrder.getItem();
                            log.info("[" + orderId + "] AddItemToOrder(" + item + ")");
                            CompletableFuture<Optional<Order>> future = repository.find(orderId);
                            pipe(future.thenCompose(option ->
                                            option
                                                    .map(order -> addItem(order, item))
                                                    .orElseGet(() -> orderNotFound(orderId))
                                    ),
                                    getContext().getDispatcher()).to(getSender());
                        })
                .match(GetOrder.class, getOrder -> {
                            log.info("[" + orderId + "] getOrder()");
                            CompletableFuture<Optional<Order>> future = repository.find(orderId);
                            pipe(future.thenCompose(option ->
                                            option
                                                    .map(CompletableFuture::completedFuture)
                                                    .orElseGet(() -> orderNotFound(orderId))
                                    ),
                                    getContext().getDispatcher()).to(getSender());

                        })
                .build();
    }

    private CompletableFuture<OrderOpened> openOrder(OrderId orderId, Server server, Table table) {
        Vector<OrderItem> items = new Vector<>();
        Order order = new Order(orderId, server, table, items);
        CompletableFuture<Order> update = repository.update(order);
        CompletableFuture<OrderOpened> orderOpenedCompletableFuture = update.thenApply(OrderOpened::new);
        return orderOpenedCompletableFuture;
    }

    private CompletableFuture<ItemAddedToOrder> addItem(Order order, OrderItem orderItem) {
        Order newOrder = order.withItem(orderItem);
        CompletableFuture<Order> update = repository.update(newOrder);
        CompletableFuture<ItemAddedToOrder> itemAddedToOrderCompletableFuture = update.thenApply(ItemAddedToOrder::new);
        return itemAddedToOrderCompletableFuture;
    }

    private <T> CompletableFuture<T> duplicateOrder(OrderId orderId) {
        return CompletableFuture.failedFuture(new DuplicateOrderException(orderId));
    }

    private <T> CompletableFuture<T> orderNotFound(OrderId orderId) {
        return CompletableFuture.failedFuture(new OrderNotFoundException(orderId));
    }

    static ShardRegion.MessageExtractor messageExtractor(int maxShards) {
        return new ShardRegion.MessageExtractor(){
            @Override
            public String entityId(Object message) {
                if(message instanceof Envelope) {
                    return ((Envelope) message).getOrderId().getValue().toString();
                }
                return null;
            }

            @Override
            public Object entityMessage(Object message) {
                if(message instanceof Envelope) {
                    return ((Envelope) message).getCommand();
                }
                return null;
            }

            @Override
            public String shardId(Object message) {
                if(message instanceof Envelope) {
                    return String.valueOf(Math.abs(((Envelope) message).getOrderId().hashCode() % maxShards));
                }
                return null;
            }
        };
    }

    static class Envelope implements SerializableMessage {
        private final OrderId orderId;
        private final OrderActor.Command command;

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

    interface Command extends SerializableMessage {
    }

    interface Event extends SerializableMessage {
    }

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

    static class GetOrder implements Command {
    }

    static class OrderNotFoundException extends IllegalStateException {
        private final OrderId orderId;

        public OrderNotFoundException(OrderId orderId) {
            super("Order Not Found: " + orderId);
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
            super("Duplicate Order: " + orderId);
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
