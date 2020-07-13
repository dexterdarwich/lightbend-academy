package com.reactivebbq.orders;

import akka.actor.Status;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.http.javadsl.testkit.TestRouteResult;
import akka.testkit.TestProbe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.reactivebbq.orders.OrderHelpers.*;

class OrderRoutesTest extends JUnitRouteTest {

    private TestProbe orders;
    private TestRoute route;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        systemResource().before();
        orders = new TestProbe(system(), "orders");
        OrderRoutes routes = new OrderRoutes(orders.ref());
        route = testRoute(routes.createRoutes());
        objectMapper = new ObjectMapper();
    }

    @Test
    void post_to_order_shouldCreateANewOrder() throws JsonProcessingException {
        Order order = generateOrder();
        OrderActor.OpenOrder request = new OrderActor.OpenOrder(order.getServer(), order.getTable());

        String json = objectMapper.writeValueAsString(request);

        TestRouteResult result = route.run(HttpRequest.POST("/order/").withEntity(ContentTypes.APPLICATION_JSON, json));

        OrderActor.Envelope msg = orders.expectMsgClass(OrderActor.Envelope.class);
        assertEquals(OrderActor.OpenOrder.class, msg.getCommand().getClass());
        assertEquals(request, msg.getCommand());

        orders.reply(new OrderActor.OrderOpened(order));

        result.assertStatusCode(StatusCodes.OK)
            .assertContentType(ContentTypes.APPLICATION_JSON)
            .assertEntityAs(Jackson.unmarshaller(Order.class), order);
    }

    @Test
    void get_to_order_id_shouldReturnTheOrder() {
        Order order = generateOrder();

        TestRouteResult result = route.run(HttpRequest.GET("/order/"+order.getId().getValue().toString()));

        OrderActor.Envelope msg = orders.expectMsgClass(OrderActor.Envelope.class);
        assertEquals(OrderActor.GetOrder.class, msg.getCommand().getClass());

        orders.reply(order);

        result.assertStatusCode(StatusCodes.OK)
                .assertContentType(ContentTypes.APPLICATION_JSON)
                .assertEntityAs(Jackson.unmarshaller(Order.class), order);
    }

    @Test
    void get_to_order_id_shouldReturnAMeaningfulErrorIfTheOrderDoesntExist() {
        OrderId orderId = generateOrderId();

        TestRouteResult result = route.run(HttpRequest.GET("/order/"+orderId.getValue().toString()));

        OrderActor.OrderNotFoundException expectedError = new OrderActor.OrderNotFoundException(orderId);

        OrderActor.Envelope msg = orders.expectMsgClass(OrderActor.Envelope.class);
        assertEquals(OrderActor.GetOrder.class, msg.getCommand().getClass());

        orders.reply(new Status.Failure(expectedError));

        result.assertStatusCode(StatusCodes.NOT_FOUND)
                .assertContentType(ContentTypes.TEXT_PLAIN_UTF8)
                .assertEntity(expectedError.getMessage());
    }

    @Test
    void post_to_order_id_items_shouldAddAnItemToTheOrder() throws JsonProcessingException {
        OrderItem orderItem = generateOrderItem();
        Order order = generateOrder().withItem(orderItem);
        OrderActor.AddItemToOrder request = new OrderActor.AddItemToOrder(orderItem);

        String json = objectMapper.writeValueAsString(request);

        TestRouteResult result = route.run(HttpRequest.POST("/order/"+order.getId().getValue().toString()+"/items")
            .withEntity(ContentTypes.APPLICATION_JSON, json));

        OrderActor.Envelope msg = orders.expectMsgClass(OrderActor.Envelope.class);
        assertEquals(OrderActor.AddItemToOrder.class, msg.getCommand().getClass());
        assertEquals(request, msg.getCommand());

        orders.reply(new OrderActor.ItemAddedToOrder(order));

        result.assertStatusCode(StatusCodes.OK)
            .assertContentType(ContentTypes.APPLICATION_JSON)
            .assertEntityAs(Jackson.unmarshaller(Order.class), order);
    }

    @Test
    void post_to_order_id_items_shouldReturnAMeaningfulErrorIfTheOrderDoesntExist() throws JsonProcessingException {
        OrderId orderId = generateOrderId();
        OrderItem orderItem = generateOrderItem();
        OrderActor.AddItemToOrder request = new OrderActor.AddItemToOrder(orderItem);

        String json = objectMapper.writeValueAsString(request);

        TestRouteResult result = route.run(HttpRequest.POST("/order/"+orderId.getValue().toString()+"/items")
            .withEntity(ContentTypes.APPLICATION_JSON, json));

        OrderActor.OrderNotFoundException expectedError = new OrderActor.OrderNotFoundException(orderId);

        orders.expectMsgClass(OrderActor.Envelope.class);
        orders.reply(new Status.Failure(expectedError));

        result.assertStatusCode(StatusCodes.NOT_FOUND)
            .assertContentType(ContentTypes.TEXT_PLAIN_UTF8)
            .assertEntity(expectedError.getMessage());
    }

    @AfterEach
    void teardown() {
        systemResource().after();
    }

}
