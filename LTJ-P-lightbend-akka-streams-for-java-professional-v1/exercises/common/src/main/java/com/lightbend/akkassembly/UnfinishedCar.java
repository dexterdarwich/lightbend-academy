package com.lightbend.akkassembly;

import java.time.Duration;
import java.util.*;

import static com.lightbend.akkassembly.Busy.*;

class UnfinishedCar {
    private static final Random random = new Random(4);
    private static final int busyTimeScale = 10;
    private static final Duration paintTime = calculateRandomMillis();
    private static final Duration installEngineTime = calculateRandomMillis();
    private static final Duration installWheelsTime = calculateRandomMillis();
    private static final Duration installUpgradeTime = calculateRandomMillis();

    private final Optional<Color> color;
    private final Optional<Engine> engine;
    private final List<Wheel> wheels;
    private final Optional<Upgrade> upgrade;

    Optional<Color> getColor() {
        return color;
    }

    Optional<Engine> getEngine() {
        return engine;
    }

    List<Wheel> getWheels() {
        return new Vector<>(wheels);
    }

    Optional<Upgrade> getUpgrade() {
        return upgrade;
    }

    UnfinishedCar() {
        color = Optional.empty();
        engine = Optional.empty();
        wheels = new Vector<>();
        upgrade = Optional.empty();
    }

    UnfinishedCar(
            Optional<Color> color,
            Optional<Engine> engine,
            List<Wheel> wheels,
            Optional<Upgrade> upgrade
    ) {
        this.color = color;
        this.engine = engine;
        this.wheels = new Vector<>(wheels);
        this.upgrade = upgrade;
    }

    private static Duration calculateRandomMillis() {
        return Duration.ofMillis(random.nextInt(busyTimeScale));
    }

    UnfinishedCar paint(Color color) {

        busy(Duration.ofMillis(0));

        return new UnfinishedCar(
                Optional.of(color),
                engine,
                wheels,
                upgrade
        );
    }

    UnfinishedCar installEngine(Engine engine) {

        busy(Duration.ofMillis(0));

        return new UnfinishedCar(
                color,
                Optional.of(engine),
                wheels,
                upgrade
        );
    }

    UnfinishedCar installWheels(List<Wheel> wheels) {

        busy(Duration.ofMillis(0));

        return new UnfinishedCar(
                color,
                engine,
                wheels,
                upgrade
        );
    }

    UnfinishedCar installUpgrade(Upgrade upgrade) {

        busy(Duration.ofMillis(0));

        return new UnfinishedCar(
                color,
                engine,
                wheels,
                Optional.of(upgrade)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnfinishedCar that = (UnfinishedCar) o;
        return Objects.equals(color, that.color) &&
                Objects.equals(engine, that.engine) &&
                Objects.equals(wheels, that.wheels) &&
                Objects.equals(upgrade, that.upgrade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, engine, wheels, upgrade);
    }
}
