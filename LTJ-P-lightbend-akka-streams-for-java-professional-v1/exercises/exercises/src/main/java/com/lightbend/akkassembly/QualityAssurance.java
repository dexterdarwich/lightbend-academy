package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Flow;

public class QualityAssurance {
    private final Flow<UnfinishedCar, Car, NotUsed> inspect;

    public QualityAssurance() {
        inspect = Flow.of(UnfinishedCar.class).filter(this::isValidCar).map(car ->
                new Car(new SerialNumber(),
                        car.getColor().get(),
                        car.getEngine().get(),
                        car.getWheels(),
                        car.getUpgrade())
        );
    }

    public Flow<UnfinishedCar, Car, NotUsed> getInspect() {
        return inspect;
    }

    private boolean isValidCar(UnfinishedCar car) {
        return car.getColor().isPresent() &&
                car.getEngine().isPresent() &&
                car.getWheels().size() == 4;
    }
}
