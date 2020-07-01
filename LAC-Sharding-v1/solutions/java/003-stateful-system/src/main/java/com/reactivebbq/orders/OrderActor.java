package com.reactivebbq.orders;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;
import akka.actor.Status;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.Patterns.pipe;

public class OrderActor extends AbstractActorWithStash {
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

    private static class OrderLoaded {
        private Optional<Order> order;

        public OrderLoaded(Optional<Order> order) {
            this.order = order;
        }

        public Optional<Order> getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderLoaded that = (OrderLoaded) o;
            return Objects.equals(order, that.order);
        }

        @Override
        public int hashCode() {
            return Objects.hash(order);
        }
    }

    static ShardRegion.MessageExtractor messageExtractor(int maxShards) {
        return new ShardRegion.MessageExtractor() {
            @Override
            public String entityId(Object message) {
                if (message instanceof Envelope) {
                    return ((Envelope) message).getOrderId().getValue().toString();
                } else {
                    return null;
                }
            }

            @Override
            public Object entityMessage(Object message) {
                if (message instanceof Envelope) {
                    return ((Envelope) message).getCommand();
                } else {
                    return null;
                }
            }

            @Override
            public String shardId(Object message) {
                if (message instanceof Envelope) {
                    return String.valueOf(Math.abs(((Envelope) message).getOrderId().hashCode() % maxShards));
                } else {
                    return null;
                }
            }
        };
    }

    static Props props(OrderRepository repository) {
        return Props.create(
            OrderActor.class,
            () -> new OrderActor(repository)
        );
    }

    private final OrderId orderId;
    private final OrderRepository repository;
    private final LoggingAdapter log;

    private Optional<Order> state;

    public OrderActor(OrderRepository repository) {
        this.repository = repository;
        this.log = Logging.getLogger(getContext().getSystem(), this);
        this.orderId = new OrderId(UUID.fromString(getContext().getSelf().path().name()));
        this.state = Optional.empty();

        pipe(repository.find(orderId).thenApply(OrderLoaded::new), getContext().getDispatcher())
            .to(getSelf());
    }

    @Override
    public Receive createReceive() {
        return loading();
    }

    private Receive running() {
        return receiveBuilder()
            .match(
                OpenOrder.class,
                cmd -> {
                    log.info("["+orderId+"] OpenOrder("+cmd.getServer()+","+cmd.getTable()+")");

                    if(state.isPresent()) {
                        pipe(
                            duplicateOrder(orderId),
                            getContext().getDispatcher()
                        ).to(getSender());
                    } else {
                        getContext().become(waiting());
                        pipe(
                            openOrder(orderId, cmd.getServer(), cmd.getTable()),
                            getContext().getDispatcher()
                        ).to(getSelf(), getSender());
                    }
                }
            )
            .match(
                AddItemToOrder.class,
                cmd -> {
                    log.info("["+orderId+"] AddItemToOrder("+cmd.getItem()+")");

                    if(state.isPresent()) {
                        getContext().become(waiting());
                        pipe(
                            addItem(state.get(), cmd.getItem()),
                            getContext().getDispatcher()
                        ).to(getSelf(), getSender());
                    } else {
                        pipe(
                            orderNotFound(orderId),
                            getContext().getDispatcher()
                        ).to(getSender());
                    }
                }
            )
            .match(
                GetOrder.class,
                cmd -> {
                    log.info("["+orderId+"] GetOrder()");

                    if(state.isPresent()) {
                        getSender().tell(state.get(), getSelf());
                    } else {
                        pipe(
                            orderNotFound(orderId),
                            getContext().getDispatcher()
                        ).to(getSender());
                    }
                }
            )
            .build();
    }

    private Receive waiting() {
        return receiveBuilder()
            .match(
                OrderOpened.class,
                evt -> {
                    state = Optional.of(evt.getOrder());
                    unstashAll();
                    getSender().tell(evt, getSelf());
                    getContext().become(running());
                }
            )
            .match(
                ItemAddedToOrder.class,
                evt -> {
                    state = Optional.of(evt.getOrder());
                    unstashAll();
                    getSender().tell(evt, getSelf());
                    getContext().become(running());
                }
            )
            .match(
                Status.Failure.class,
                failure -> {
                    log.error(failure.cause(), "["+orderId+"] FAILURE: "+failure.cause().getMessage());
                    getSender().tell(failure, getSelf());
                    throw new RuntimeException(failure.cause());
                }
            )
            .matchAny(ignore -> stash())
            .build();
    }

    private Receive loading() {
        return receiveBuilder()
            .match(
                OrderLoaded.class,
                cmd -> {
                    unstashAll();
                    state = cmd.getOrder();
                    getContext().become(running());
                }
            )
            .match(
                Status.Failure.class,
                failure -> {
                    log.error(failure.cause(), "["+orderId+"] FAILURE: "+failure.cause().getMessage());
                    throw new RuntimeException(failure.cause());
                }
            )
            .matchAny(ignore -> stash())
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
