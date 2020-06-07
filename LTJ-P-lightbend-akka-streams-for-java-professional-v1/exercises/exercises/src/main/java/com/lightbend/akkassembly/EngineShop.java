package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Source;

import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

public class EngineShop {
    private final Source<Shipment, NotUsed> shipments;

    public EngineShop(int shipmentSize) {
        Stream<Shipment> shipmentStream = Stream.generate(() -> {
            List<Engine> engines = new Vector<>();
            for (int i = 0; i < shipmentSize; i++) {
                engines.add(new Engine());
            }
            return new Shipment(engines);
        });
        shipments = Source.cycle(() -> shipmentStream.iterator());
    }

    public Source<Shipment, NotUsed> getShipments() {
        return shipments;
    }
}
