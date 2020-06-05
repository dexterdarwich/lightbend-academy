package com.lightbend.akkassembly;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import org.junit.After;
import org.junit.Before;

public class AkkaSpec {

    ActorSystem system;
    Materializer materializer;

    @Before
    public void setup() {
        system = ActorSystem.create();
        materializer = ActorMaterializer.create(system);
    }

    @After
    public void teardown() {
        system.terminate();
    }
}
