package com.lightbend.akkassembly;

import akka.Done;
import akka.NotUsed;
import akka.event.LoggingAdapter;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class Auditor {
    private final Sink<Car, CompletionStage<Integer>> count;

    public Auditor() {
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
}
