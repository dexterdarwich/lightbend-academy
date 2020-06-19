package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.DelayOverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Concat;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.EventFilter;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class AuditorTest extends AkkaSpec {

    private final List<Object> oneToTen = IntStream
        .rangeClosed(1, 10)
        .boxed()
        .collect(Collectors.toList());

    private List<Car> generateCars(int quantity) {
        List<Car> cars = new Vector<>();
        for(int i = 0; i < quantity; i++) {
            List<Wheel> wheels = new Vector<>();
            wheels.add(new Wheel());
            wheels.add(new Wheel());
            wheels.add(new Wheel());
            wheels.add(new Wheel());

            Car car = new Car(
                    new SerialNumber(),
                    Color.fromHex("000000"),
                    new Engine(),
                    wheels,
                    Optional.empty()
            );

            cars.add(car);
        }

        return cars;
    }

    @Test
    public void count_shouldReturnZeroIfTheStreamIsEmpty() {
        Auditor auditor = new Auditor(materializer);

        int count = Source.<Car>empty()
            .runWith(auditor.getCount(), materializer)
            .toCompletableFuture()
            .join();

        assertEquals(0, count);
    }

    @Test
    public void count_shouldCountElementsInTheStream() {
        Auditor auditor = new Auditor(materializer);

        int count = Source.from(generateCars(10))
            .runWith(auditor.getCount(), materializer)
            .toCompletableFuture()
            .join();

        assertEquals(10, count);
    }

    @Test
    public void log_shouldLogNothingIfTheSourceIsEmpty() {
        Auditor auditor = new Auditor(materializer);

        EventFilter
            .debug(null, null, "", null, 0)
            .intercept(() ->
                Source.empty()
                    .runWith(auditor.log(system.log()), materializer)
                    .toCompletableFuture()
                    .join()
                , system);
    }

    @Test
    public void log_shouldLogAllElementsToTheLoggingAdapter() {
        Auditor auditor = new Auditor(materializer);

        EventFilter
            .debug(null, null, "", null, 10)
            .intercept(() ->
                Source.from(oneToTen)
                    .runWith(auditor.log(system.log()), materializer)
                    .toCompletableFuture()
                    .join()
                , system);
    }

    @Test
    public void log_shouldLogTheExactElement() {
        Auditor auditor = new Auditor(materializer);

        EventFilter
            .debug("Message", null, "", null, 1)
            .intercept(() ->
                Source.single((Object) "Message")
                    .runWith(auditor.log(system.log()), materializer)
                    .toCompletableFuture()
                    .join()
                , system);
    }

    @Test
    public void sample_shouldDoNothingIfTheSourceIsEmpty() {
        Auditor auditor = new Auditor(materializer);
        Duration sampleSize = Duration.ofMillis(100);

        List<Car> cars = Source.<Car>empty()
                .via(auditor.sample(sampleSize))
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        assertEquals(0, cars.size());
    }

    @Test
    public void sample_shouldReturnAllElementsIfTheyAppearInTheSamplePeriod() {
        Auditor auditor = new Auditor(materializer);
        Duration sampleSize = Duration.ofMillis(100);
        int expectedCars = 10;

        List<Car> cars = Source.from(generateCars(expectedCars))
            .via(auditor.sample(sampleSize))
            .runWith(Sink.seq(), materializer)
            .toCompletableFuture()
            .join();

        assertEquals(expectedCars, cars.size());
    }

    @Test
    public void sample_shouldIgnoreElementsThatAppearOutsideTheExpectedSamplePeriod() {
        Auditor auditor = new Auditor(materializer);
        Duration sampleSize = Duration.ofMillis(100);
        int expectedCars = 5;

        Source<Car, NotUsed> fast = Source.from(generateCars(expectedCars));
        Source<Car, NotUsed> slow = Source.from(generateCars(expectedCars))
                .initialDelay(sampleSize.multipliedBy(2));

        List<Car> cars = fast.concat(slow)
                .via(auditor.sample(sampleSize))
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        assertEquals(expectedCars, cars.size());
    }

    @Test
    public void audit_shouldReturnZeroIfThereAreNoCars() {
        Auditor auditor = new Auditor(materializer);

        int count = auditor.audit(Source.empty(), Duration.ofMillis(100))
            .toCompletableFuture()
            .join();

        assertEquals(0, count);
    }

    @Test
    public void audit_shouldReturnAllCarsInTheSamplePeriod() {
        Auditor auditor = new Auditor(materializer);

        int expectedQuantity = 10;
        Source<Car, NotUsed> cars = Source.from(generateCars(expectedQuantity));

        int count = auditor.audit(cars, Duration.ofMillis(100))
            .toCompletableFuture()
            .join();

        assertEquals(expectedQuantity, count);
    }

    @Test
    public void audit_shouldLimitTheCarsToThoseInTheSamplePeriod() {
        Auditor auditor = new Auditor(materializer);

        int expectedQuantity = 10;
        Duration sampleSize = Duration.ofMillis(100);

        Source<Car, NotUsed> expectedCars = Source
                .from(generateCars(expectedQuantity));

        Source<Car, NotUsed> unexpectedCars = Source
                .from(generateCars(expectedQuantity))
                .initialDelay(sampleSize.multipliedBy(2));

        Source<Car, NotUsed> cars = expectedCars.concat(unexpectedCars);

        int count = auditor.audit(cars, Duration.ofMillis(100))
            .toCompletableFuture()
            .join();

        assertEquals(expectedQuantity, count);
    }
}
