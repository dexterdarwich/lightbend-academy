package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.FlowShape;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Balance;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Merge;

public class UpgradeShop {
    private final Flow<UnfinishedCar, UnfinishedCar, NotUsed> installUpgrades;

    public UpgradeShop() {
        installUpgrades = Flow.fromGraph(GraphDSL.create(builder -> {
            UniformFanOutShape<UnfinishedCar, UnfinishedCar> balance = builder.add(Balance.create(3));
            UniformFanInShape<UnfinishedCar, UnfinishedCar> merge = builder.add(Merge.create(3));

            FlowShape<UnfinishedCar, UnfinishedCar> upgradeToDX = builder.add(
                    Flow.of(UnfinishedCar.class).map(car -> car.installUpgrade(Upgrade.DX)));
            FlowShape<UnfinishedCar, UnfinishedCar> upgradeToSport = builder.add(
                    Flow.of(UnfinishedCar.class).map(car -> car.installUpgrade(Upgrade.Sport)));

            builder.from(balance.out(0)).via(upgradeToDX).toInlet(merge.in(0));
            builder.from(balance.out(1)).via(upgradeToSport).toInlet(merge.in(1));
            builder.from(balance.out(2)).toInlet(merge.in(2));

            return FlowShape.of(balance.in(), merge.out());
        }));
    }

    public Flow<UnfinishedCar, UnfinishedCar, NotUsed> getInstallUpgrades() {
        return installUpgrades;
    }
}
