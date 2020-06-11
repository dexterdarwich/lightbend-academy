package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

public class EngineShop {
    private final Source<Shipment, NotUsed> shipments;
    private final Source<Engine, NotUsed> engines;
    private final Flow<UnfinishedCar, UnfinishedCar, NotUsed> installEngine;

    public EngineShop(int shipmentSize) {
        shipments = Source.fromIterator(() -> Stream.generate(() -> {
            List<Engine> engines = new Vector<>();
            for (int i = 0; i < shipmentSize; i++) {
                engines.add(new Engine());
            }
            return new Shipment(engines);
        }).iterator());

        engines = shipments.mapConcat(Shipment::getEngines);
        installEngine = Flow.of(UnfinishedCar.class).zip(engines)
                .map(carAndEngine -> carAndEngine.first().installEngine(carAndEngine.second()));
    }

    public Source<Engine, NotUsed> getEngines() {
        return engines;
    }

    public Source<Shipment, NotUsed> getShipments() {
        return shipments;
    }

    public Flow<UnfinishedCar, UnfinishedCar, NotUsed> getInstallEngine() {
        return installEngine;
    }
}
