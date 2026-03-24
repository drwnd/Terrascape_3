package core.utils;

import org.joml.Vector3f;

public final class MathUtils {

    private MathUtils() {

    }

    public static float absMin(float a, float b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }

    public static boolean isInteger(String string, int radix) {
        if (string.isEmpty()) return false;
        for (int index = 0; index < string.length(); index++) {
            char character = string.charAt(index);
            if (index == 0 && (character == '-' || character == '+')) {
                if (string.length() == 1) return false;
                continue;
            }
            if (Character.digit(character, radix) < 0) return false;
        }
        return true;
    }

    public static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public static int floor(float value) {
        return (int) Math.floor(value);
    }

    public static int floor(double value) {
        int addend = value < 0.0 ? -1 : 0;
        return (int) value + addend;
    }

    public static float fraction(float value) {
        return (float) (value - Math.floor(value));
    }

    public static double smoothInOutQuad(double x, double lowBound, double highBound) {
        // Maps centerX ∈ [lowBound, highBound] to [0, 1]
        x -= lowBound;
        x /= highBound - lowBound;

        if (x < 0.5) return 2 * x * x;
        double oneMinusX = 1 - x;
        return 1 - 2 * oneMinusX * oneMinusX;
    }

    public static int hash(int x, int z, int seed) {
        final int mask = 0x5BD1E995;
        // process first vector element
        x *= mask;
        x ^= x >> 24;
        x *= mask;
        seed *= mask;
        seed ^= x;
        // process second vector element
        z *= mask;
        z ^= z >> 24;
        z *= mask;
        seed *= mask;
        seed ^= z;
        // some final mixing
        seed ^= seed >> 13;
        seed *= mask;
        seed ^= seed >> 15;
        return seed;
    }

    public static int nextLargestPowOf2(int value) {
        int powOf2 = 1;
        while (powOf2 < value) powOf2 <<= 1;
        return powOf2;
    }

    public static int mod(long base, int modulo) {
        int value = (int) (base % modulo);
        return value < 0 ? value + modulo : value;
    }

    public static Vector3f getDirection(Vector3f rotation) {

        float rotationXRadians = (float) Math.toRadians(rotation.y);
        float rotationYRadians = (float) Math.toRadians(rotation.x);

        float x = (float) Math.sin(rotationXRadians);
        float y = (float) -Math.sin(rotationYRadians);
        float z = (float) -Math.cos(rotationXRadians);

        float normalizer = (float) Math.sqrt(1 - y * y);

        x *= normalizer;
        z *= normalizer;

        return new Vector3f(x, y, z);
    }

    public static Vector3f getHorizontalDirection(Vector3f rotation) {
        float rotationXRadians = (float) Math.toRadians(rotation.y);

        float x = (float) Math.sin(rotationXRadians);
        float z = (float) -Math.cos(rotationXRadians);

        return new Vector3f(x, 0.0F, z);
    }

    public static double round(float value, int decimals) {
        double multiplier = Math.pow(10, decimals);
        int multipliedValue = (int) (value * multiplier);
        return multipliedValue / multiplier;
    }
}
