package com.lightbend.akkassembly;

import java.util.Objects;
import java.util.UUID;

class SerialNumber {
    private final UUID value;

    UUID getValue() {
        return value;
    }

    SerialNumber() {
        this.value = UUID.randomUUID();
    }

    SerialNumber(UUID value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerialNumber that = (SerialNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
