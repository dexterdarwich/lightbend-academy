package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.ActorAttributes;
import akka.stream.Supervision;
import akka.stream.javadsl.Flow;
import akka.japi.function.Function;

public class QualityAssurance {
    static class CarFailedInspection extends IllegalStateException {
        public CarFailedInspection(UnfinishedCar car) {
            super("Car Failed Inspection: " + car);
        }
    }

    private final Flow<UnfinishedCar, Car, NotUsed> inspect;

    public QualityAssurance() {
        Function<Throwable, Supervision.Directive> decider = ex -> {
            if(ex instanceof CarFailedInspection) {
                return Supervision.resume();
            } else {
                return Supervision.stop();
            }
        };

        inspect = Flow.of(UnfinishedCar.class)
                .map(car -> {
                    if(isValidCar(car))
                        return new Car(
                                new SerialNumber(),
                                car.getColor().get(),
                                car.getEngine().get(),
                                car.getWheels(),
                                car.getUpgrade()
                        );
                    else
                        throw new CarFailedInspection(car);
                }).withAttributes(ActorAttributes.withSupervisionStrategy(decider));
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
