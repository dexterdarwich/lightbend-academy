package com.reactivebbq.orders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class OrderItem {
    private final String name;
    private final String specialInstructions;

    @JsonCreator
    public OrderItem(@JsonProperty("name") String name, @JsonProperty("specialInstructions") String specialInstructions) {
        this.name = name;
        this.specialInstructions = specialInstructions;
    }

    public String getName() {
        return name;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(name, orderItem.name) &&
                Objects.equals(specialInstructions, orderItem.specialInstructions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, specialInstructions);
    }


    @Override
    public String toString() {
        return "OrderItem{" +
                "name='" + name + '\'' +
                ", specialInstructions='" + specialInstructions + '\'' +
                '}';
    }
}
