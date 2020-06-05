package com.lightbend.akkassembly;

import org.junit.Test;

import static org.junit.Assert.*;

public class EngineTest {

    @Test
    public void equals_shouldReturnTrueForIdenticalEngines() {
        SerialNumber serialNumber = new SerialNumber();
        Engine e1 = new Engine(serialNumber);
        Engine e2 = new Engine(serialNumber);

        assertEquals(e1, e2);
    }

    @Test
    public void equals_shouldReturnFalseForDifferentEngines() {
        Engine e1 = new Engine(new SerialNumber());
        Engine e2 = new Engine(new SerialNumber());

        assertNotEquals(e1, e2);
    }

}
