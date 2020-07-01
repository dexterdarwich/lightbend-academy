package com.reactivebbq.orders;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;
import java.util.UUID;

public class OrderId {

    public static OrderId fromString(String value) throws IllegalArgumentException {
        return new OrderId(UUID.fromString(value));
    }

    private UUID value;

    public OrderId() {
        this.value = UUID.randomUUID();
    }

    public OrderId(UUID value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderId{" +
                "value=" + value.toString() +
                '}';
    }

    public UUID getValue() {
        return value;
    }

}
