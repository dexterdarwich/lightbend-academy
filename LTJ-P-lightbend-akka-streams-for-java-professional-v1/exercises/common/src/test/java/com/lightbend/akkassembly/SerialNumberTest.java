package com.lightbend.akkassembly;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class SerialNumberTest {

    @Test
    public void equals_shouldReturnTrueForIdenticalSerialNumbers() {
        UUID uuid = UUID.randomUUID();

        SerialNumber s1 = new SerialNumber(uuid);
        SerialNumber s2 = new SerialNumber(uuid);

        assertEquals(s1, s2);
    }

    @Test
    public void equals_shouldReturnFalseForDifferentSerialNumbers() {
        SerialNumber s1 = new SerialNumber();
        SerialNumber s2 = new SerialNumber();

        assertNotEquals(s1, s2);
    }

}
