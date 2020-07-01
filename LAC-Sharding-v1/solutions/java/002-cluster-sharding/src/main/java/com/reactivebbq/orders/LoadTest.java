package com.reactivebbq.orders;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.Materializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static akka.pattern.Patterns.pipe;

public class LoadTest {
    private static final Logger logger = LoggerFactory.getLogger(LoadTest.class);
    private static ActorSystem system;
    private static Materializer materializer;
    private static Config config;
    private static List<Integer> ports;
    private static Duration testDuration;
    private static int parallelism;
    private static Duration rampUpTime;

    public static void main(String[] args) {
        loadConfigOverrides(args);
        loadConfig();
        initializeActorSystem();
        run();
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

    private static void loadConfig() {
        config = ConfigFactory.load("loadtest.conf");

        ports = config.getIntList("reactive-bbq.orders.ports");
        testDuration = Duration.ofMillis(config.getDuration("load-test.duration", TimeUnit.MILLISECONDS));
        parallelism = config.getInt("load-test.parallelism");
        rampUpTime = Duration.ofMillis(config.getDuration("load-test.ramp-up-time", TimeUnit.MILLISECONDS));
    }

    private static void initializeActorSystem() {
        system = ActorSystem.create("LoadTest", config);
        materializer = Materializer.createMaterializer(system);
    }

    private static void run() {
        logger.info("Creating "+parallelism+" simulations");

        for(int i = 0; i < parallelism; i++) {
            try {
                Thread.sleep(rampUpTime.toMillis() / parallelism);
            } catch (InterruptedException ex) {
                logger.error("Error while sleeping", ex);
            }

            ActorRef sim = system.actorOf(Simulation.create(ports, system, materializer));
            system.getScheduler().scheduleOnce(testDuration, sim, new Simulation.Stop(), system.getDispatcher(), Actor.noSender());
        }

        system.scheduler().scheduleOnce(testDuration.plusSeconds(15), () -> {
            materializer.shutdown();
            system.terminate();

        }, system.getDispatcher());
    }
}

class Simulation extends AbstractActor {
    public static class Start {}
    public static class Stop {}

    public static Props create(List<Integer> targetPorts, ActorSystem system, Materializer materializer) {
        return Props.create(Simulation.class, () -> new Simulation(targetPorts, system, materializer));
    }

    private final List<Integer> targetPorts;
    private final Http http;
    private final ActorSystem system;
    private final Materializer materializer;
    private final Executor executor;
    private final LoggingAdapter log;

    private long startTime;

    public Simulation(List<Integer> targetPorts, ActorSystem system, Materializer materializer) {
        http = Http.get(getContext().getSystem());

        startTime = System.currentTimeMillis();

        log = Logging.getLogger(getContext().getSystem(), this);

        this.system = system;
        this.executor = system.dispatcher();
        this.materializer = materializer;
        this.targetPorts = targetPorts;

        getContext().getSelf().tell(new Start(), getContext().getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Start.class, (msg) -> {
                startTime = System.currentTimeMillis();
                pipe(run(), getContext().getDispatcher()).to(getContext().getSelf());
            })
            .match(Stop.class, (msg) ->
                getContext().stop(getContext().getSelf())
            )
            .match(Order.class, (msg) -> {
                long deltaTime = System.currentTimeMillis() - startTime;
                log.info("Order completed in "+deltaTime+" ms");
                getContext().getSelf().tell(new Start(), getContext().getSelf());
            })
            .match(Status.Failure.class, (msg) ->
                log.info("Order Failed: "+msg.cause().getMessage())
            )
            .build();
    }

    private int port() {
        List<Integer> shuffledPorts = new ArrayList<>(targetPorts);
        Collections.shuffle(shuffledPorts);

        return shuffledPorts.get(0);
    }

    private CompletableFuture<Order> run() {
        return openOrder().thenComposeAsync(order ->
            retrieveOrder(order.getId()),
            executor
        ).thenComposeAsync(order ->
            addItem(order.getId(), "Steak"),
            executor
        ).thenComposeAsync(order ->
            retrieveOrder(order.getId()),
            executor
        ).thenComposeAsync(order ->
            addItem(order.getId(), "Salad"),
            executor
        ).thenComposeAsync(order ->
            retrieveOrder(order.getId()),
            executor
        ).thenComposeAsync(order ->
            addItem(order.getId(), "Milk"),
            executor
        ).thenComposeAsync(order ->
            retrieveOrder(order.getId()),
            executor
        ).thenComposeAsync(order ->
            addItem(order.getId(), "Cheesecake"),
            executor
        ).thenComposeAsync(order ->
            retrieveOrder(order.getId()),
            executor
        );
    }

    private CompletableFuture<Order> openOrder() {
        OrderActor.OpenOrder command = new OrderActor.OpenOrder(new Server("Server"), new Table(5));
        String url = "http://localhost:"+port()+"/order";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(command);

            HttpRequest request = HttpRequest.POST(url).withEntity(ContentTypes.APPLICATION_JSON, json);

            return http.singleRequest(request)
                .thenComposeAsync(response ->
                    response.entity().toStrict(5000, materializer),
                    executor
                ).thenApplyAsync(response -> {
                    try {
                        return objectMapper.readValue(response.getData().toArray(), Order.class);
                    } catch (IOException ex) {
                        throw new CompletionException(ex);
                    }
                }, executor).toCompletableFuture();
        } catch (JsonProcessingException ex) {
            log.error(ex, "Unable to build request.");
            getContext().stop(getContext().getSelf());
            return null;
        }
    }

    private CompletableFuture<Order> retrieveOrder(OrderId id) {
        String url = "http://localhost:"+port()+"/order/"+id.getValue().toString();

        ObjectMapper objectMapper = new ObjectMapper();

        HttpRequest request = HttpRequest.GET(url);

        return http.singleRequest(request)
            .thenComposeAsync(response ->
                response.entity().toStrict(5000, materializer),
                executor
            ).thenApplyAsync(response -> {
                try {
                    return objectMapper.readValue(response.getData().toArray(), Order.class);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }, executor).toCompletableFuture();
    }

    private CompletableFuture<Order> addItem(OrderId id, String itemName) {
        OrderActor.AddItemToOrder command = new OrderActor.AddItemToOrder(new OrderItem(itemName, "None"));
        String url = "http://localhost:"+port()+"/order/"+id.getValue().toString()+"/items";

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String json = objectMapper.writeValueAsString(command);

            HttpRequest request = HttpRequest.POST(url).withEntity(ContentTypes.APPLICATION_JSON, json);

            return http.singleRequest(request)
                .thenComposeAsync(response ->
                    response.entity().toStrict(5000, materializer),
                    executor
                ).thenApplyAsync(response -> {
                    try {
                        return objectMapper.readValue(response.getData().toArray(), Order.class);
                    } catch (IOException ex) {
                        throw new CompletionException(ex);
                    }
                }, executor).toCompletableFuture();
        } catch (JsonProcessingException ex) {
            log.error(ex, "Unable to build request.");
            getContext().stop(getContext().getSelf());
            return null;
        }
    }
}
