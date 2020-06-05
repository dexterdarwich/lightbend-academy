package com.lightbend.akkassembly;

import java.util.Objects;

class Color {
    private final int red;
    private final int green;
    private final int blue;

    int getRed() {
        return red;
    }

    int getGreen() {
        return green;
    }

    int getBlue() {
        return blue;
    }

    Color(int red, int green, int blue) {
        assert red >= 0 && red <= 255;
        assert green >= 0 && green <= 255;
        assert blue >= 0 && blue <= 255;

        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return red == color.red &&
                green == color.green &&
                blue == color.blue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue);
    }

    static Color fromHex(String hexString) {
        return new Color(
            Integer.parseInt(hexString.substring(0, 2), 16),
            Integer.parseInt(hexString.substring(2, 4), 16),
            Integer.parseInt(hexString.substring(4, 6), 16)
        );
    }
}
