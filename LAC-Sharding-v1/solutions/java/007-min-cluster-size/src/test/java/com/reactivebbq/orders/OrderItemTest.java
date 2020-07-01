package com.reactivebbq.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    public void equals_shouldReturnTrue_forTheSameOrderItem() {
        OrderItem o1 = new OrderItem("Steak", "medium");
        OrderItem o2 = new OrderItem("Steak", "medium");

        assertEquals(o1, o2);
    }

    @Test
    public void equals_shouldReturnFalse_forADifferentOrderItem() {
        OrderItem o1 = new OrderItem("Steak", "medium");
        OrderItem o2 = new OrderItem("Beyond Steak", "medium");
        OrderItem o3 = new OrderItem("Steak", "rare");

        assertNotEquals(o1, o2);
        assertNotEquals(o1, o3);
        assertNotEquals(o2, o3);
    }

}
