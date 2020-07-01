package com.reactivebbq.orders;

import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.testkit.TestProbe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.reactivebbq.orders.OrderHelpers.*;

class OrderRoutesTest extends JUnitRouteTest {

    private TestProbe orders;
    private TestRoute route;

    @BeforeEach
    void setup() {
        systemResource().before();
        orders = new TestProbe(system(), "orders");
        OrderRoutes routes = new OrderRoutes(orders.ref());
        route = testRoute(routes.createRoutes());
    }

    @Test
    void post_to_order_shouldFail() throws Exception {
        Server server = generateServer();
        Table table = generateTable();
        OrderActor.OpenOrder openOrder = new OrderActor.OpenOrder(server, table);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(openOrder);

        route.run(HttpRequest.POST("/order/").withEntity(ContentTypes.APPLICATION_JSON, json))
            .assertStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
    }

    @Test
    void get_to_order_id_shouldFail() {
        Order order = generateOrder();

        route.run(HttpRequest.GET("/order/"+order.getId().getValue().toString()))
            .assertStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
    }

    @Test
    void post_to_order_id_items_shouldFail() throws Exception {
        OrderItem item = generateOrderItem();
        Order order = generateOrder().withItem(item);
        OrderActor.AddItemToOrder addItemToOrder = new OrderActor.AddItemToOrder(item);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(addItemToOrder);

        route.run(HttpRequest.POST("/order/"+order.getId().getValue().toString()+"/items/").withEntity(ContentTypes.APPLICATION_JSON, json))
            .assertStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);

    }

    @AfterEach
    void teardown() {
        systemResource().after();
    }

}
