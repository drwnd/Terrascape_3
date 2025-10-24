package game.utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Utils {

    public static long getChunkId(int chunkX, int chunkY, int chunkZ) {
        return (long) (chunkX & MAX_CHUNKS_XZ) << 40 | (long) (chunkY & MAX_CHUNKS_Y) << 24 | chunkZ & MAX_CHUNKS_XZ;
    }

    public static int getChunkIndex(int chunkX, int chunkY, int chunkZ) {
        chunkX %= RENDERED_WORLD_WIDTH;
        if (chunkX < 0) chunkX += RENDERED_WORLD_WIDTH;

        chunkY %= RENDERED_WORLD_HEIGHT;
        if (chunkY < 0) chunkY += RENDERED_WORLD_HEIGHT;

        chunkZ %= RENDERED_WORLD_WIDTH;
        if (chunkZ < 0) chunkZ += RENDERED_WORLD_WIDTH;

        return (chunkX * RENDERED_WORLD_WIDTH + chunkZ) * RENDERED_WORLD_HEIGHT + chunkY;
    }

    public static boolean outsideChunkKeepDistance(int playerChunkX, int playerChunkY, int playerChunkZ, int chunkX, int chunkY, int chunkZ) {
        return Math.abs(chunkX - playerChunkX) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE + 1
                || Math.abs(chunkZ - playerChunkZ) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE + 1
                || Math.abs(chunkY - playerChunkY) > RENDER_DISTANCE_Y + RENDER_KEEP_DISTANCE + 1;
    }

    public static boolean outsideRenderKeepDistance(int playerChunkX, int playerChunkY, int playerChunkZ, int chunkX, int chunkY, int chunkZ) {
        return Math.abs(chunkX - playerChunkX) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE
                || Math.abs(chunkZ - playerChunkZ) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE
                || Math.abs(chunkY - playerChunkY) > RENDER_DISTANCE_Y + RENDER_KEEP_DISTANCE;
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

    public static int makeEven(int value) {
        return value & 0xFFFFFFFE;
    }

    public static int makeOdd(int value) {
        return value | 1;
    }

    public static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public static int floor(float value) {
        int addend = value < 0.0F ? -1 : 0;
        return (int) value + addend;
    }

    public static int floor(double value) {
        int addend = value < 0.0 ? -1 : 0;
        return (int) value + addend;
    }

    public static float fraction(float number) {
        int addend = number < 0.0F ? 1 : 0;
        return (number - (int) number) + addend;
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

    public static Vector3i offsetByNormal(Vector3i value, int side) {
        switch (side) {
            case NORTH -> value.add(0, 0, 1);
            case TOP -> value.add(0, 1, 0);
            case WEST -> value.add(1, 0, 0);
            case SOUTH -> value.add(0, 0, -1);
            case BOTTOM -> value.add(0, -1, 0);
            case EAST -> value.add(-1, 0, 0);
        }
        return value;
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

    private Utils() {
    }
}
