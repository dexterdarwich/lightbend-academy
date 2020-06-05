package com.lightbend.akkassembly;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import static org.junit.Assert.*;

public class UnfinishedCarTest {

    @Test
    public void equals_shouldReturnTrueForIdenticalCars() {
        Color color = Color.fromHex("FFFFFF");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade = Upgrade.Sport;

        UnfinishedCar c1 = new UnfinishedCar(Optional.of(color), Optional.of(engine), wheels, Optional.of(upgrade));
        UnfinishedCar c2 = new UnfinishedCar(Optional.of(color), Optional.of(engine), wheels, Optional.of(upgrade));

        assertEquals(c1, c2);
    }

    @Test
    public void equals_shouldReturnFalseIfColorIsDifferent() {
        Color color1 = Color.fromHex("FFFFFF");
        Color color2 = Color.fromHex("000000");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade = Upgrade.Sport;

        UnfinishedCar c1 = new UnfinishedCar(Optional.of(color1), Optional.of(engine), wheels, Optional.of(upgrade));
        UnfinishedCar c2 = new UnfinishedCar(Optional.of(color2), Optional.of(engine), wheels, Optional.of(upgrade));
        UnfinishedCar c3 = new UnfinishedCar(Optional.empty(), Optional.of(engine), wheels, Optional.of(upgrade));

        assertNotEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c2, c3);
    }

    @Test
    public void equals_shouldReturnFalseIfUpgradeIsDifferent() {
        Color color = Color.fromHex("FFFFFF");
        Engine engine = new Engine();

        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        Upgrade upgrade1 = Upgrade.Sport;
        Upgrade upgrade2 = Upgrade.DX;

        UnfinishedCar c1 = new UnfinishedCar(Optional.of(color), Optional.of(engine), wheels, Optional.of(upgrade1));
        UnfinishedCar c2 = new UnfinishedCar(Optional.of(color), Optional.of(engine), wheels, Optional.of(upgrade2));
        UnfinishedCar c3 = new UnfinishedCar(Optional.of(color), Optional.of(engine), wheels, Optional.empty());

        assertNotEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c2, c3);
    }

    @Test
    public void paint_shouldReturnAPaintedCopyOfTheCar() {
        Color color = Color.fromHex("FFFFFF");

        UnfinishedCar unpainted = new UnfinishedCar();
        UnfinishedCar painted = unpainted.paint(color);

        assertNotEquals(unpainted, painted);
        assertTrue(painted.getColor().isPresent());
        assertEquals(color, painted.getColor().get());
    }

    @Test
    public void installEngine_shouldReturnACopyOfTheCarWithTheEngineInstalled() {
        Engine engine = new Engine();

        UnfinishedCar noEngine = new UnfinishedCar();
        UnfinishedCar withEngine = noEngine.installEngine(engine);

        assertNotEquals(noEngine, withEngine);
        assertTrue(withEngine.getEngine().isPresent());
        assertEquals(engine, withEngine.getEngine().get());
    }

    @Test
    public void installWheels_shouldReturnACopyOfTheCarWithTheWheelsInstalled() {
        List<Wheel> wheels = new Vector<>();
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());
        wheels.add(new Wheel());

        UnfinishedCar noWheels = new UnfinishedCar();
        UnfinishedCar withWheels = noWheels.installWheels(wheels);

        assertNotEquals(noWheels, withWheels);
        assertEquals(wheels, withWheels.getWheels());
    }

    @Test
    public void installUpgrade_shouldReturnACopyOfTheCarWithTheUpgradeInstalled() {
        Upgrade upgrade = Upgrade.DX;

        UnfinishedCar noUpgrade = new UnfinishedCar();
        UnfinishedCar withUpgrade = noUpgrade.installUpgrade(upgrade);

        assertNotEquals(noUpgrade, withUpgrade);
        assertTrue(withUpgrade.getUpgrade().isPresent());
        assertEquals(upgrade, withUpgrade.getUpgrade().get());
    }
}
