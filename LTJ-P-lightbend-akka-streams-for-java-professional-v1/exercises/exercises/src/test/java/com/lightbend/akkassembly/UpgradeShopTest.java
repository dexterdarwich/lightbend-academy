package com.lightbend.akkassembly;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class UpgradeShopTest extends AkkaSpec {
    @Test
    public void upgrade_shouldUpgradeTheCorrectRatioOfCars() {
        int numCars = 12;
        UpgradeShop upgradeShop = new UpgradeShop();

        List<Optional<Upgrade>> upgrades = Source.repeat(new UnfinishedCar())
                .take(numCars)
                .via(upgradeShop.getInstallUpgrades())
                .map(car -> car.getUpgrade())
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .join();

        long totalNoUpgrade = upgrades.stream()
                .filter(u -> !u.isPresent())
                .count();
        long totalDX = upgrades.stream()
                .filter(u -> u.isPresent() && u.get() == Upgrade.DX)
                .count();
        long totalSport = upgrades.stream()
                .filter(u -> u.isPresent() && u.get() == Upgrade.Sport)
                .count();

        assertEquals(numCars/3, totalNoUpgrade);
        assertEquals(numCars/3, totalDX);
        assertEquals(numCars/3, totalSport);
    }
}
