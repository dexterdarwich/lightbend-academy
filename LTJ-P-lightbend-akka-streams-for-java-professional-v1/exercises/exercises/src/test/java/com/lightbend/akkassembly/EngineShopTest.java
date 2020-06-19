package com.lightbend.akkassembly;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class EngineShopTest extends AkkaSpec {

    @Test
    public void shipments_shouldEmitASeriesOfUniqueShipments() {
        int shipmentSize = 10;
        int numberToRequest = 5;

        EngineShop engineShop = new EngineShop(shipmentSize);

        List<Shipment> shipments = engineShop.getShipments()
                .take(numberToRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        for(Shipment shipment : shipments) {
            assertEquals(shipmentSize, new HashSet<>(shipment.getEngines()).size());
        }
    }

    @Test
    public void shipments_shouldEmitUniqueEnginesFromOneShipmentToTheNext() {
        int shipmentSize = 1;
        int numberToRequest = 5;

        EngineShop engineShop = new EngineShop(shipmentSize);

        List<Engine> engines = engineShop.getShipments()
                .mapConcat(Shipment::getEngines)
                .take(numberToRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        assertEquals(numberToRequest, new HashSet<>(engines).size());
    }

    @Test
    public void engines_shouldFlattenTheShipmentsIntoUniqueEngines() {
        int shipmentSize = 10;
        EngineShop engineShop = new EngineShop(shipmentSize);

        List<Engine> engines = engineShop.getEngines()
            .take(10)
            .runWith(Sink.seq(), materializer)
            .toCompletableFuture()
            .join();

        assertEquals(10, engines.size());
        assertEquals(10, new HashSet<>(engines).size());
    }

    @Test
    public void installEngine_shouldInstallAnEngineInTheCar() {
        EngineShop engineShop = new EngineShop(10);

        List<UnfinishedCar> cars = Source.repeat(new UnfinishedCar())
            .take(10)
            .via(engineShop.getInstallEngine())
            .runWith(Sink.seq(), materializer)
            .toCompletableFuture()
            .join();

        for(UnfinishedCar car : cars) {
            assertTrue(car.getEngine().isPresent());
        }
    }
}
