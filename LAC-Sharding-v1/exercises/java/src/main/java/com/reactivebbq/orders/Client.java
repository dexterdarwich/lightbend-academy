package com.reactivebbq.orders;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.Materializer;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class Client {
    private static ActorSystem system;
    private static Materializer materializer;
    private static Executor executor;

    private static Iterator<Integer> ports;

    public static void main(String[] args) {
        if(args.length == 0) {
            printUsageAndExit();
        }

        initializePorts();
        initializeActorSystem();

        parseCommand(args);
    }

    private static void initializeActorSystem() {
        Config config = ConfigFactory.load("client.conf");
        system = ActorSystem.create("Client", config);
        materializer = Materializer.createMaterializer(system);
        executor = system.dispatcher();
    }

    private static void initializePorts() {
        Integer[] portArray = { 8000, 8001, 8002 };
        List<Integer> portList = Arrays.asList(portArray);
        Collections.shuffle(portList);
        ports = portList.iterator();
    }

    private static int getPort() {
        if(ports.hasNext()) {
            return ports.next();
        } else {
            System.out.println("Tried all available ports without success.");
            system.terminate();
            return 0;
        }
    }

    private static String toJson(OrderActor.Command cmd) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writeValueAsString(cmd);
        } catch (JsonProcessingException ex) {
            // Realistically, this shouldn't happen.
            system.log().error(ex, "Failed to serialize command.");
            system.terminate();
            return "{}";
        }
    }

    private static void parseCommand(String[] args) {
        CompletableFuture<String> result = CompletableFuture.completedFuture("");

        switch(args[0]) {
            case "open":
                result = runIf(args.length == 3, () ->
                    openOrder(args[1], Integer.parseInt(args[2]))
                );
                break;
            case "add":
                result = runIf(args.length == 4, () ->
                    addItem(args[1], args[2], args[3])
                );

                break;

            case "find":
                result = runIf(args.length == 2, () ->
                    findOrder(args[1])
                );

                break;
            default:
                printUsageAndExit();
        }

        result.whenCompleteAsync((value, exception) -> {
            if(exception != null) {
                System.out.println("Failure: "+exception.getMessage());
                system.terminate();
            } else {
                System.out.println(value);
                materializer.shutdown();
                system.terminate();

                system.getWhenTerminated().toCompletableFuture().join();
            }
        }, executor);
    }

    private static CompletableFuture<String> openOrder(String server, int table) {
        OrderActor.OpenOrder command = new OrderActor.OpenOrder(new Server(server), new Table(table));
        String json = toJson(command);

        String url = "http://localhost:"+getPort()+"/order";

        HttpRequest request = HttpRequest.POST(url).withEntity(ContentTypes.APPLICATION_JSON, json);

        return Http.get(system)
                .singleRequest(request)
                .thenComposeAsync(result -> result.entity().toStrict(5000, materializer), executor)
                .thenApplyAsync(entity -> CompletableFuture.completedFuture(entity.getData().utf8String()), executor)
                .exceptionally(ex -> {
                    system.log().warning("Attempt to connect to "+url+" failed. Retrying.");
                    return openOrder(server, table);
                })
                .thenComposeAsync(future -> future, executor)
                .toCompletableFuture();
    }

    private static CompletableFuture<String> addItem(String orderId, String itemName, String specialInstructions) {
        OrderActor.AddItemToOrder command = new OrderActor.AddItemToOrder(new OrderItem(itemName, specialInstructions));
        String json = toJson(command);

        String url = "http://localhost:"+getPort()+"/order/"+orderId+"/items";

        HttpRequest request = HttpRequest.POST(url).withEntity(ContentTypes.APPLICATION_JSON, json);

        return Http.get(system)
                .singleRequest(request)
                .thenComposeAsync(result -> result.entity().toStrict(5000, materializer), executor)
                .thenApplyAsync(entity -> CompletableFuture.completedFuture(entity.getData().utf8String()), executor)
                .exceptionally(ex -> {
                    system.log().warning("Attempt to connect to "+url+" failed. Retrying.");
                    return addItem(orderId, itemName, specialInstructions);
                })
                .thenComposeAsync(future -> future, executor)
                .toCompletableFuture();
    }

    private static CompletableFuture<String> findOrder(String orderId) {
        String url = "http://localhost:"+getPort()+"/order/"+orderId;

        HttpRequest request = HttpRequest.GET(url);

        return Http.get(system)
                .singleRequest(request)
                .thenComposeAsync(result -> result.entity().toStrict(5000, materializer), executor)
                .thenApplyAsync(entity -> CompletableFuture.completedFuture(entity.getData().utf8String()), executor)
                .exceptionally(ex -> {
                    system.log().warning("Attempt to connect to "+url+" failed. Retrying.");
                    return findOrder(orderId);
                })
                .thenComposeAsync(future -> future, executor)
                .toCompletableFuture();
    }

    private static CompletableFuture<String> runIf(boolean requirement, Supplier<CompletableFuture<String>> run) {
        if(!requirement) {
            printUsageAndExit();
        }

        return run.get();
    }

    private static void printUsageAndExit() {
        System.out.println("USAGE:");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"com.reactivebbq.orders.Client\" -Dexec.args=\"open <serverName> <tableNumber>\"");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"com.reactivebbq.orders.Client\" -Dexec.args=\"add <orderId> <itemName> <specialInstructions>\"");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"com.reactivebbq.orders.Client\" -Dexec.args=\"find <orderId>\"");

        if(system != null)
            system.terminate();

        System.exit(1);
    }
}
