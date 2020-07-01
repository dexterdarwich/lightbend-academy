package com.reactivebbq.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    public void equals_shouldReturnTrue_forTheSameTable() {
        Table t1 = new Table(5);
        Table t2 = new Table(5);

        assertEquals(t1, t2);
    }

    @Test
    public void equals_shouldReturnFalse_forADifferentTable() {
        Table t1 = new Table(5);
        Table t2 = new Table(6);

        assertNotEquals(t1, t2);
    }

}
