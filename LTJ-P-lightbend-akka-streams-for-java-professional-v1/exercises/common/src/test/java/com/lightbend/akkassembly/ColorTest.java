package com.lightbend.akkassembly;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorTest {

    @Test
    public void fromHex_shouldReturnCorrectColors() {
        Color white = Color.fromHex("FFFFFF");
        assertEquals(255, white.getRed());
        assertEquals(255, white.getGreen());
        assertEquals(255, white.getBlue());

        Color red = Color.fromHex("FF0000");
        assertEquals(255, red.getRed());
        assertEquals(0, red.getGreen());
        assertEquals(0, red.getBlue());

        Color green = Color.fromHex("00FF00");
        assertEquals(0, green.getRed());
        assertEquals(255, green.getGreen());
        assertEquals(0, green.getBlue());

        Color blue = Color.fromHex("0000FF");
        assertEquals(0, blue.getRed());
        assertEquals(0, blue.getGreen());
        assertEquals(255, blue.getBlue());

        Color black = Color.fromHex("000000");
        assertEquals(0, black.getRed());
        assertEquals(0, black.getGreen());
        assertEquals(0, black.getBlue());
    }

    @Test
    public void equals_shouldReturnTrueForSameColors() {
        Color c1 = Color.fromHex("FFFFFF");
        Color c2 = Color.fromHex("FFFFFF");

        assertEquals(c1, c2);
    }

    @Test
    public void equals_shouldReturnFalseForDifferentColors() {
        Color c1 = Color.fromHex("FFFFFF");
        Color c2 = Color.fromHex("FFFF00");
        Color c3 = Color.fromHex("FF00FF");
        Color c4 = Color.fromHex("00FFFF");
        Color c5 = Color.fromHex("000000");

        assertNotEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c1, c4);
        assertNotEquals(c1, c5);
    }
}
