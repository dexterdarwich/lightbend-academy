package com.lightbend.akkassembly;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import static org.junit.Assert.*;

public class CarTest {

    @Test
    public void equals_shouldReturnTrueForIdenticalCars() {
        SerialNumber serialNumber = new SerialNumber();
        Color color = Color.fromHex("FFFFFF");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade = Upgrade.Sport;

        Car c1 = new Car(serialNumber, color, engine, wheels, Optional.of(upgrade));
        Car c2 = new Car(serialNumber, color, engine, wheels, Optional.of(upgrade));

        assertEquals(c1, c2);
    }

    @Test
    public void equals_shouldReturnFalseIfSerialIsDifferent() {
        SerialNumber serialNumber1 = new SerialNumber();
        SerialNumber serialNumber2 = new SerialNumber();
        Color color = Color.fromHex("FFFFFF");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade = Upgrade.Sport;

        Car c1 = new Car(serialNumber1, color, engine, wheels, Optional.of(upgrade));
        Car c2 = new Car(serialNumber2, color, engine, wheels, Optional.of(upgrade));

        assertNotEquals(c1, c2);
    }

    @Test
    public void equals_shouldReturnFalseIfColorIsDifferent() {
        SerialNumber serialNumber = new SerialNumber();
        Color color1 = Color.fromHex("FFFFFF");
        Color color2 = Color.fromHex("000000");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade = Upgrade.Sport;

        Car c1 = new Car(serialNumber, color1, engine, wheels, Optional.of(upgrade));
        Car c2 = new Car(serialNumber, color2, engine, wheels, Optional.of(upgrade));

        assertNotEquals(c1, c2);
    }

    @Test
    public void equals_shouldReturnFalseIfUpgradeIsDifferent() {
        SerialNumber serialNumber = new SerialNumber();
        Color color = Color.fromHex("FFFFFF");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade1 = Upgrade.Sport;
        Upgrade upgrade2 = Upgrade.DX;

        Car c1 = new Car(serialNumber, color, engine, wheels, Optional.of(upgrade1));
        Car c2 = new Car(serialNumber, color, engine, wheels, Optional.of(upgrade2));
        Car c3 = new Car(serialNumber, color, engine, wheels, Optional.empty());

        assertNotEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c2, c3);
    }

}
