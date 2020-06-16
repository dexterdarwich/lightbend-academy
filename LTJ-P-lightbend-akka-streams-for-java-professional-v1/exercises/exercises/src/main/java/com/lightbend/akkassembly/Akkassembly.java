package com.lightbend.akkassembly;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Akkassembly {

  public static void main(String[] args) {
      ActorSystem actorSystem = ActorSystem.create("akkassembly");
      Materializer materializer = ActorMaterializer.create(actorSystem);

      Duration buildTime = Duration.ofMillis(1);
      BodyShop bodyShop = new BodyShop(buildTime);

      Set<Color> paintColors = new HashSet<>(Arrays.asList(
          Color.fromHex("FFFFFF"),
          Color.fromHex("000000"),
          Color.fromHex("FF00FF")
      ));
      PaintShop paintShop = new PaintShop(paintColors);

      WheelShop wheelShop = new WheelShop();

      int shipmentSize = 10;
      EngineShop engineShop = new EngineShop(shipmentSize);

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

      long startTime = System.currentTimeMillis();

      factory.orderCars(1000).thenAccept(cars -> {
        long orderTime = System.currentTimeMillis() - startTime;

        System.out.println(cars.size() + " cars produced in "+orderTime+"ms");

        actorSystem.terminate();
      });
  }
}
