package com.reactivebbq.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    @Test
    public void equals_shouldReturnTrue_forTheSameServer() {
        Server s1 = new Server("Kate");
        Server s2 = new Server("Kate");

        assertEquals(s1, s2);
    }

    @Test
    public void equals_shouldReturnFalse_forDifferentServers() {
        Server s1 = new Server("Kate");
        Server s2 = new Server("Jon");

        assertNotEquals(s1, s2);
    }

}
