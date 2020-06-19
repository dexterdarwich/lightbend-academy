package com.lightbend.akkassembly;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class WheelShopTest extends AkkaSpec {

    @Test
    public void wheels_shouldReturnASeriesOfWheels() {
        int numberToRequest = 100;
        WheelShop wheelShop = new WheelShop();

        List<Wheel> wheels = wheelShop.getWheels()
                .take(numberToRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        assertEquals(numberToRequest, wheels.size());
    }

    @Test
    public void installWheels_shouldInstallFourWheelsOnEachCar() {
        WheelShop wheelShop = new WheelShop();

        List<UnfinishedCar> cars = Source.repeat(new UnfinishedCar())
            .take(10)
            .via(wheelShop.getInstallWheels())
            .runWith(Sink.seq(), materializer)
            .toCompletableFuture()
            .join();

        for(UnfinishedCar car : cars) {
            assertEquals(4, car.getWheels().size());
        }
    }
}
