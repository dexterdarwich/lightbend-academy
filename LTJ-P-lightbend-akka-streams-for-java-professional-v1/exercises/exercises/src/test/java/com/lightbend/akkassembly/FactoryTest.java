package com.lightbend.akkassembly;

import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class FactoryTest extends AkkaSpec {

    @Test
    public void orderCars_shouldReturnTheRequestedQuantityOfCars() {

        int expectedQuantity = 12;
        Color color = Color.fromHex("000000");
        BodyShop bodyShop = new BodyShop(Duration.ofMillis(1));
        PaintShop paintShop = new PaintShop(new HashSet<>(Arrays.asList(color)));
        EngineShop engineShop = new EngineShop(20);
        WheelShop wheelShop = new WheelShop();
        UpgradeShop upgradeShop = new UpgradeShop();
        QualityAssurance qualityAssurance = new QualityAssurance();

        Factory factory = new Factory(
            bodyShop,
            paintShop,
            engineShop,
            wheelShop,
            qualityAssurance,
            upgradeShop,
            materializer
        );

        List<Car> cars = factory.orderCars(expectedQuantity)
            .toCompletableFuture()
            .join();

        assertEquals(expectedQuantity, cars.size());

        cars.forEach(car -> {
            assertEquals(color, car.getColor());
            assertEquals(4, car.getWheels().size());
        });

        Set<Engine> engines = cars.stream()
                .map(Car::getEngine)
                .collect(Collectors.toSet());

        assertEquals(expectedQuantity, engines.size());

        List<Optional<Upgrade>> upgrades = cars.stream()
                .map(Car::getUpgrade)
                .collect(Collectors.toList());

        long totalNoUpgrade = upgrades.stream()
                .filter(u -> !u.isPresent())
                .count();
        long totalDX = upgrades.stream()
                .filter(u -> u.isPresent() && u.get() == Upgrade.DX)
                .count();
        long totalSport = upgrades.stream()
                .filter(u -> u.isPresent() && u.get() == Upgrade.Sport)
                .count();

        assertEquals(expectedQuantity/3, totalNoUpgrade);
        assertEquals(expectedQuantity/3, totalDX);
        assertEquals(expectedQuantity/3, totalSport);
    }

}
