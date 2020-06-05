package com.lightbend.akkassembly;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;

class Car {
    private final SerialNumber serialNumber;
    private final Color color;
    private final Engine engine;
    private final List<Wheel> wheels;
    private final Optional<Upgrade> upgrade;

    SerialNumber getSerialNumber() {
        return serialNumber;
    }

    Color getColor() {
        return color;
    }

    Engine getEngine() {
        return engine;
    }

    List<Wheel> getWheels() {
        return new Vector<>(wheels);
    }

    Optional<Upgrade> getUpgrade() {
        return upgrade;
    }

    Car(SerialNumber serialNumber, Color color, Engine engine, List<Wheel> wheels, Optional<Upgrade> upgrade) {
        assert wheels.size() == 4;

        this.serialNumber = serialNumber;
        this.color = color;
        this.engine = engine;
        this.wheels = wheels;
        this.upgrade = upgrade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(serialNumber, car.serialNumber) &&
                Objects.equals(color, car.color) &&
                Objects.equals(engine, car.engine) &&
                Objects.equals(wheels, car.wheels) &&
                Objects.equals(upgrade, car.upgrade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber, color, engine, wheels, upgrade);
    }
}
