package com.lightbend.akkassembly;

import akka.Done;
import akka.NotUsed;
import akka.event.LoggingAdapter;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class Auditor {
    private final Sink<Car, CompletionStage<Integer>> count;
    private final Materializer materializer;

    public Auditor(Materializer materializer) {
        this.materializer = materializer;
        this.count = Sink.fold(0, (count, ignore) -> count + 1);
    }

    public Sink<Car, CompletionStage<Integer>> getCount() {
        return count;
    }

    public Sink<Object, CompletionStage<Done>> log(LoggingAdapter loggingAdapter) {
        return Sink.foreach(m -> loggingAdapter.debug("{}", m));
    }

    public Flow<Car, Car, NotUsed> sample(Duration sampleSize) {
        return Flow.of(Car.class).takeWithin(sampleSize);
    }

    CompletionStage<Integer> audit(Source<Car, NotUsed> cars, Duration sampleSize) {
        return cars.via(sample(sampleSize)).toMat(count, Keep.right()).run(materializer);
    }
}
