package com.lightbend.akkassembly;

import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class Factory {
    private final BodyShop bodyShop;
    private final PaintShop paintShop;
    private final EngineShop engineShop;
    private final WheelShop wheelShop;
    private final QualityAssurance qualityAssurance;
    private final UpgradeShop upgradeShop;
    private final Materializer materializer;

    public Factory(BodyShop bodyShop, PaintShop paintShop, EngineShop engineShop, WheelShop wheelShop, QualityAssurance qualityAssurance, UpgradeShop upgradeShop, Materializer materializer) {
        this.bodyShop = bodyShop;
        this.paintShop = paintShop;
        this.engineShop = engineShop;
        this.wheelShop = wheelShop;
        this.qualityAssurance = qualityAssurance;
        this.upgradeShop = upgradeShop;
        this.materializer = materializer;
    }

    CompletionStage<List<Car>> orderCars(int quantity) {
        return bodyShop.getCars()
                .via(paintShop.getPaint())
                .via(engineShop.getInstallEngine())
                .async()
                .via(wheelShop.getInstallWheels())
                .via(upgradeShop.getInstallUpgrades())
                .via(qualityAssurance.getInspect())
                .take(quantity)
                .toMat(Sink.seq(), Keep.right())
                .run(materializer);
    }
}
