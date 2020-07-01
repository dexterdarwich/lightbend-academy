package com.reactivebbq.orders;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.routing.RoundRobinPool;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static ActorSystem system;
    private static Materializer materializer;
    private static ActorRef orders;
    private static OrderRepository orderRepository;

    public static void main(String[] args) {
        loadConfigOverrides(args);

        initializeActorSystem();
        initializeRepository();
        initializeActors();
        initializeHttpServer();
    }

    private static void loadConfigOverrides(String[] args) {
        String regex = "-D(\\S+)=(\\S+)";
        Pattern pattern = Pattern.compile(regex);

        for (String arg : args) {
            Matcher matcher = pattern.matcher(arg);

            while(matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                logger.info("Config Override: "+key+" = "+value);
                System.setProperty(key, value);
            }
        }
    }

    private static void initializeActorSystem() {
        system = ActorSystem.create("Orders");
        materializer = Materializer.createMaterializer(system);
    }

    private static void initializeRepository() {
        Executor blockingExecutor = system.dispatchers().lookup("blocking-dispatcher");
        orderRepository = new SQLOrderRepository(blockingExecutor);
    }

    private static void initializeActors() {
        orders = system.actorOf(new RoundRobinPool(100).props(OrderActor.props(orderRepository)));
    }

    private static void initializeHttpServer() {
        OrderRoutes routes = new OrderRoutes(orders);

        int httpPort = system.settings()
            .config()
            .getInt("akka.http.server.default-http-port");

        Http.get(system).bindAndHandle(
            routes.createRoutes().flow(system, materializer),
            ConnectHttp.toHost("localhost", httpPort),
            materializer
        );
    }
}
