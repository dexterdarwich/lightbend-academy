package com.reactivebbq.orders;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OrderIdTest {

    @Test
    void equals_shouldReturnTrueForSameId() {
        UUID baseId = UUID.randomUUID();
        OrderId id1 = new OrderId(baseId);
        OrderId id2 = new OrderId(baseId);

        assertEquals(id1, id2);
    }

    @Test
    void equals_shouldReturnFalseForDifferentId() {
        OrderId id1 = new OrderId(UUID.randomUUID());
        OrderId id2 = new OrderId(UUID.randomUUID());

        assertNotEquals(id1, id2);
    }


    @Test
    public void defaultConstructor_should_generateAUniqueIdEachTimeItIsCalled() {
        int numIds = 10;
        HashSet<OrderId> ids = new HashSet<>();

        for(int i = 0; i < numIds; i++) {
            ids.add(new OrderId());
        }

        assertEquals(numIds, ids.size());
    }
}
