package com.lightbend.akkassembly;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

import static org.junit.Assert.*;

public class QualityAssuranceTest extends AkkaSpec {
    private final UnfinishedCar completedCar = new UnfinishedCar(
        Optional.of(Color.fromHex("FFFFFF")),
        Optional.of(new Engine()),
        Arrays.asList(new Wheel(), new Wheel(), new Wheel(), new Wheel()),
        Optional.of(Upgrade.DX)
    );


    @Test
    public void inspect_shouldRejectCarsWithNoColor() {
        QualityAssurance qa = new QualityAssurance();

        UnfinishedCar car = new UnfinishedCar(
                Optional.empty(),
                completedCar.getEngine(),
                completedCar.getWheels(),
                completedCar.getUpgrade()
        );

        Optional<Car> result = Source.single(car)
            .via(qa.getInspect())
            .runWith(Sink.headOption(), materializer)
            .toCompletableFuture()
            .join();

        assertFalse(result.isPresent());
    }

    @Test
    public void inspect_shouldRejectCarsWithNoEngine() {
        QualityAssurance qa = new QualityAssurance();

        UnfinishedCar car = new UnfinishedCar(
                completedCar.getColor(),
                Optional.empty(),
                completedCar.getWheels(),
                completedCar.getUpgrade()
        );

        Optional<Car> result = Source.single(car)
                .via(qa.getInspect())
                .runWith(Sink.headOption(), materializer)
                .toCompletableFuture()
                .join();

        assertFalse(result.isPresent());
    }

    @Test
    public void inspect_shouldRejectCarsWithNoWheels() {
        QualityAssurance qa = new QualityAssurance();

        UnfinishedCar car = new UnfinishedCar(
                completedCar.getColor(),
                completedCar.getEngine(),
                new Vector<>(),
                completedCar.getUpgrade()
        );

        Optional<Car> result = Source.single(car)
                .via(qa.getInspect())
                .runWith(Sink.headOption(), materializer)
                .toCompletableFuture()
                .join();

        assertFalse(result.isPresent());
    }

    @Test
    public void inspect_shouldAcceptCarsThatAreComplete() {
        QualityAssurance qa = new QualityAssurance();

        UnfinishedCar car = new UnfinishedCar(
                completedCar.getColor(),
                completedCar.getEngine(),
                completedCar.getWheels(),
                completedCar.getUpgrade()
        );

        Optional<Car> result = Source.single(car)
                .via(qa.getInspect())
                .runWith(Sink.headOption(), materializer)
                .toCompletableFuture()
                .join();

        assertTrue(result.isPresent());
        assertEquals(completedCar.getColor().get(), result.get().getColor());
        assertEquals(completedCar.getEngine().get(), result.get().getEngine());
        assertEquals(completedCar.getWheels(), result.get().getWheels());
        assertEquals(completedCar.getUpgrade(), result.get().getUpgrade());
    }
}

//class QualityAssuranceTest extends FreeSpec with AkkaSpec {
//
//  "inspect" - {
//

//    "should accept cars that are complete" in {
//      val qa = new QualityAssurance()
//
//      val completeCar = UnfinishedCar(
//        color = Some(Color("000000")),
//        engine = Some(Engine()),
//        wheels = Seq.fill(4)(Wheel())
//      )
//      val incompleteCar = UnfinishedCar()
//
//      val cars = Source(Seq(completeCar, completeCar, incompleteCar, completeCar))
//
//      val sink = cars.via(qa.inspect).runWith(TestSink.probe[Car])
//      sink.request(10)
//      sink.expectNextN(3)
//      sink.expectComplete()
//    }
//  }
//}
