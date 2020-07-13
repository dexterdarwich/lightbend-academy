package com.reactivebbq.orders;

import akka.actor.ActorRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.Route;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.ask;

import static akka.http.javadsl.server.PathMatchers.segment;

class OrderRoutes extends AllDirectives {

    private final ActorRef orderActors;
    private final Duration timeout = Duration.ofSeconds(5);
    private final ExceptionHandler handleExceptions;

    public OrderRoutes(ActorRef orderActors) {
        this.orderActors = orderActors;
        this.handleExceptions = ExceptionHandler.newBuilder()
                .match(OrderActor.OrderNotFoundException.class, ex ->
                        complete(StatusCodes.NOT_FOUND, ex.getMessage())
                )
                .matchAny(ex ->
                        complete(StatusCodes.INTERNAL_SERVER_ERROR, ex.getMessage())
                )
                .build();
    }

    public Route createRoutes() {
        return handleExceptions(handleExceptions, () ->
                pathPrefix("order", () ->
                        concat(
                                pathEndOrSingleSlash(() ->
                                        post(() ->
                                                entity(Jackson.unmarshaller(OrderActor.OpenOrder.class), cmd ->
                                                        openOrder(cmd)
                                                )
                                        )
                                ),
                                pathPrefix(segment(), (orderId) ->
                                        concat(
                                                pathPrefix("items", () ->
                                                        pathEndOrSingleSlash(() ->
                                                                post(() ->
                                                                        entity(
                                                                                Jackson.unmarshaller(OrderActor.AddItemToOrder.class), cmd ->
                                                                                        addItemToOrder(OrderId.fromString(orderId), cmd)
                                                                        )
                                                                )
                                                        )
                                                ),
                                                pathEndOrSingleSlash(() ->
                                                        get(() ->
                                                                findOrder(OrderId.fromString(orderId))
                                                        )
                                                )

                                        )
                                )
                        )
                )
        );
    }

    private Route onComplete(CompletionStage<Order> result) {
        return onComplete(result, maybeResult ->
                maybeResult.map(order ->
                        complete(StatusCodes.OK, order, Jackson.<Order>marshaller())
                ).get()
        );
    }

    private Route openOrder(OrderActor.OpenOrder cmd) {
        OrderId orderId = new OrderId();
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, cmd);
        CompletionStage<Object> objectCompletionStage = ask(orderActors, envelope, timeout);
        CompletionStage<OrderActor.OrderOpened> orderOpenedCompletionStage = objectCompletionStage.thenApply(object -> (OrderActor.OrderOpened) object);
        CompletionStage<Order> orderCompletionStage = orderOpenedCompletionStage.thenApply(orderOpened -> orderOpened.getOrder());
        return onComplete(orderCompletionStage);
    }

    private Route findOrder(OrderId orderId) {
        OrderActor.GetOrder getOrder = new OrderActor.GetOrder();
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, getOrder);
        CompletionStage<Object> objectCompletionStage = ask(orderActors, envelope, timeout);
        CompletionStage<Order> orderCompletionStage = objectCompletionStage.thenApply(object -> (Order) object);
        return onComplete(orderCompletionStage);
    }

    private Route addItemToOrder(OrderId orderId, OrderActor.AddItemToOrder cmd) {
        OrderActor.Envelope envelope = new OrderActor.Envelope(orderId, cmd);
        CompletionStage<Object> objectCompletionStage = ask(orderActors, envelope, timeout);
        CompletionStage<OrderActor.ItemAddedToOrder> itemAddedCompletionStage = objectCompletionStage.thenApply(object -> (OrderActor.ItemAddedToOrder) object);
        CompletionStage<Order> orderCompletionStage = itemAddedCompletionStage.thenApply(itemAddedToOrder -> itemAddedToOrder.getOrder());
        return onComplete(orderCompletionStage);
    }

}
