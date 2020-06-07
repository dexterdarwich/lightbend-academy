package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Source;

public class WheelShop {
    private final Source<Wheel, NotUsed> wheels;
    public WheelShop() {
        Wheel wheel = new Wheel();
        wheels = Source.repeat(wheel);
    }

    public Source<Wheel, NotUsed> getWheels() {
        return wheels;
    }
}
