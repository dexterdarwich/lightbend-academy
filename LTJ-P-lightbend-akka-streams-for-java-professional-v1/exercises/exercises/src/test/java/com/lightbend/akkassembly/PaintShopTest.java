package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.*;

public class PaintShopTest extends AkkaSpec {

    @Test
    public void colors_shouldRepeatEachColorInTheColorSet() {
        Set<Color> colorSet = new HashSet<>();
        colorSet.add(Color.fromHex("FFFFFF"));
        colorSet.add(Color.fromHex("000000"));
        colorSet.add(Color.fromHex("FF00FF"));

        PaintShop paintShop = new PaintShop(colorSet);

        List<Color> colors = paintShop.getColors()
                .take(colorSet.size() * 2)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        List<Color> expected = new Vector<>(colorSet);
        expected.addAll(colorSet);

        assertEquals(expected.size(), colors.size());
        assertArrayEquals(expected.toArray(), colors.toArray());
    }

    @Test
    public void paint_shouldThrowAnErrorIfThereAreNoColors() {
        PaintShop paintShop = new PaintShop(new HashSet<>());
        Source<UnfinishedCar, NotUsed> cars = Source.repeat(new UnfinishedCar());

        cars.via(paintShop.getPaint())
            .runWith(TestSink.probe(system), materializer)
            .request(10)
            .expectError();
    }

    @Test
    public void paint_shouldTerminateIfThereAreNoCars() {
        Set<Color> colorSet = new HashSet<>();
        colorSet.add(Color.fromHex("000000"));

        PaintShop paintShop = new PaintShop(colorSet);
        Source<UnfinishedCar, NotUsed> cars = Source.empty();

        List<UnfinishedCar> result = cars.via(paintShop.getPaint())
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        assertEquals(0, result.size());
    }

    @Test
    public void paint_shouldApplyPaintColorsToTheCars() {
        Color color = Color.fromHex("000000");
        Set<Color> colorSet = new HashSet<>();
        colorSet.add(color);

        PaintShop paintShop = new PaintShop(colorSet);
        Source<UnfinishedCar, NotUsed> cars = Source.repeat(new UnfinishedCar());

        List<UnfinishedCar> result = cars
                .via(paintShop.getPaint())
                .take(10)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        for(UnfinishedCar car : result) {
            assertTrue(car.getColor().isPresent());
            assertEquals(color, car.getColor().get());
        }
    }
}
