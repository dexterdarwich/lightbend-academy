package com.lightbend.akkassembly;

import org.junit.Test;

import java.util.List;
import java.util.Vector;

import static org.junit.Assert.*;

public class ShipmentTest {

    @Test
    public void equals_shouldReturnTrueForIdenticalShipments() {
        List<Engine> engines = new Vector<>();
        engines.add(new Engine());
        engines.add(new Engine());
        engines.add(new Engine());

        Shipment s1 = new Shipment(engines);
        Shipment s2 = new Shipment(engines);

        assertEquals(s1, s2);
    }

    @Test
    public void equals_shouldReturnFalseForDifferentShipments() {
        List<Engine> engines1 = new Vector<>();
        engines1.add(new Engine());
        engines1.add(new Engine());
        engines1.add(new Engine());

        List<Engine> engines2 = new Vector<>();
        engines2.add(new Engine());
        engines2.add(new Engine());
        engines2.add(new Engine());

        Shipment s1 = new Shipment(engines1);
        Shipment s2 = new Shipment(engines2);

        assertNotEquals(s1, s2);
    }

}
