package com.lightbend.akkassembly;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import java.util.Set;

public class PaintShop {
    private final Source<Color, NotUsed> colors;
    private final Flow<UnfinishedCar, UnfinishedCar, NotUsed> paint;

    public PaintShop(Set<Color> colorSet) {
        colors = Source.cycle(() -> colorSet.iterator());
        paint = Flow.of(UnfinishedCar.class).zip(colors)
                .map(carAndColor -> carAndColor.first().paint(carAndColor.second()));
    }

    public Source<Color, NotUsed> getColors() {
        return colors;
    }

    public Flow<UnfinishedCar, UnfinishedCar, NotUsed> getPaint() {
        return paint;
    }
}
