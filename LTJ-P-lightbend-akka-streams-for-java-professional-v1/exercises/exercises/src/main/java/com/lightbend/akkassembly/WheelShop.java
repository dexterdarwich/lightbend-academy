package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

public class WheelShop {
    private final Source<Wheel, NotUsed> wheels;
    private final Flow<UnfinishedCar, UnfinishedCar, NotUsed> installWheels;

    public WheelShop() {
        Wheel wheel = new Wheel();
        wheels = Source.repeat(wheel);
        installWheels = Flow.of(UnfinishedCar.class).zip(wheels.grouped(4))
                .map(carAndWheels -> carAndWheels.first().installWheels(carAndWheels.second()));
    }

    public Flow<UnfinishedCar, UnfinishedCar, NotUsed> getInstallWheels() {
        return installWheels;
    }

    public Source<Wheel, NotUsed> getWheels() {
        return wheels;
    }
}
