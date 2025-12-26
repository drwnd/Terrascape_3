package game.utils;

import core.settings.ToggleSetting;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Utils {

    public static long getChunkId(int chunkX, int chunkY, int chunkZ, int lod) {
        chunkX &= MAX_CHUNKS_XZ_MASK >> lod;
        chunkY &= MAX_CHUNKS_Y_MASK >> lod;
        chunkZ &= MAX_CHUNKS_XZ_MASK >> lod;

        return (long) chunkX << 40 | (long) chunkY << 24 | chunkZ;
    }

    public static int getChunkIndex(int chunkX, int chunkY, int chunkZ, int lod) {
        chunkX &= RENDERED_WORLD_WIDTH_MASK & MAX_CHUNKS_XZ_MASK >> lod;
        chunkY &= RENDERED_WORLD_HEIGHT_MASK & MAX_CHUNKS_Y_MASK >> lod;
        chunkZ &= RENDERED_WORLD_WIDTH_MASK & MAX_CHUNKS_XZ_MASK >> lod;

        return ((chunkX << RENDERED_WORLD_WIDTH_BITS) + chunkZ << RENDERED_WORLD_HEIGHT_BITS) + chunkY;
    }

    public static boolean outsideChunkKeepDistance(int playerChunkX, int playerChunkY, int playerChunkZ, int chunkX, int chunkY, int chunkZ, int lod) {
        return distance(chunkX - playerChunkX, MAX_CHUNKS_XZ_MASK >> lod) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE + 1
                || distance(chunkZ - playerChunkZ, MAX_CHUNKS_XZ_MASK >> lod) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE + 1
                || distance(chunkY - playerChunkY, MAX_CHUNKS_Y_MASK >> lod) > RENDER_DISTANCE_Y + RENDER_KEEP_DISTANCE + 1;
    }

    public static boolean outsideRenderKeepDistance(int playerChunkX, int playerChunkY, int playerChunkZ, int chunkX, int chunkY, int chunkZ, int lod) {
        return distance(playerChunkX - chunkX, MAX_CHUNKS_XZ_MASK >> lod) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE
                || distance(chunkZ - playerChunkZ, MAX_CHUNKS_XZ_MASK >> lod) > RENDER_DISTANCE_XZ + RENDER_KEEP_DISTANCE
                || distance(chunkY - playerChunkY, MAX_CHUNKS_Y_MASK >> lod) > RENDER_DISTANCE_Y + RENDER_KEEP_DISTANCE;
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

    public static int getWrappedPosition(int actualPosition, int reference, int wrappingDistance) {
        if (actualPosition - reference > wrappingDistance >> 1) return actualPosition - wrappingDistance;
        if (reference - actualPosition > wrappingDistance >> 1) return actualPosition + wrappingDistance;
        return actualPosition;
    }

    public static int toDisplayXZ(int xz) {
        if (ToggleSetting.FAKE_COORDINATES.value()) return xz - WORLD_SIZE_XZ / 2;
        return xz;
    }

    public static int toDisplayY(int y) {
        if (ToggleSetting.FAKE_COORDINATES.value()) return y - WORLD_SIZE_Y / 2;
        return y;
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

    public static Vector3i min(Vector3i a, Vector3i b) {
        return new Vector3i(
                org.joml.Math.min(a.x, Utils.getWrappedPosition(b.x, a.x, WORLD_SIZE_XZ)),
                org.joml.Math.min(a.y, Utils.getWrappedPosition(b.y, a.y, WORLD_SIZE_Y)),
                org.joml.Math.min(a.z, Utils.getWrappedPosition(b.z, a.z, WORLD_SIZE_XZ))
        );
    }

    public static Vector3i max(Vector3i a, Vector3i b) {
        return new Vector3i(
                org.joml.Math.max(a.x, Utils.getWrappedPosition(b.x, a.x, WORLD_SIZE_XZ)),
                org.joml.Math.max(a.y, Utils.getWrappedPosition(b.y, a.y, WORLD_SIZE_Y)),
                org.joml.Math.max(a.z, Utils.getWrappedPosition(b.z, a.z, WORLD_SIZE_XZ))
        );
    }

    public static double round(float value, int decimals) {
        double multiplier = Math.pow(10, decimals);
        int multipliedValue = (int) (value * multiplier);
        return multipliedValue / multiplier;
    }

    public static int[] zOrderCurveLookupTable(int size, int split, int shift) {
        int[] table = new int[size];
        for (int index = 0; index < size; index++) table[index] = zOrderCurveValue(index, split) << shift;
        return table;
    }

    public static int getInChunkX(int zCurveIndex) {
        return zCurveIndex >> 2 & 1 |
                zCurveIndex >> 4 & 2 |
                zCurveIndex >> 6 & 4 |
                zCurveIndex >> 8 & 8 |
                zCurveIndex >> 10 & 16 |
                zCurveIndex >> 12 & 32;
    }

    public static int getInChunkY(int zCurveIndex) {
        return zCurveIndex >> 1 & 1 |
                zCurveIndex >> 3 & 2 |
                zCurveIndex >> 5 & 4 |
                zCurveIndex >> 7 & 8 |
                zCurveIndex >> 9 & 16 |
                zCurveIndex >> 11 & 32;
    }
    public static int getInChunkZ(int zCurveIndex) {
        return zCurveIndex & 1 |
                zCurveIndex >> 2 & 2 |
                zCurveIndex >> 4 & 4 |
                zCurveIndex >> 6 & 8 |
                zCurveIndex >> 8 & 16 |
                zCurveIndex >> 10 & 32;
    }



    private static int zOrderCurveValue(int value, int split) {
        int zOrderValue = 0;
        for (int index = 0; index < 10; index++) {
            int bit = value >> index & 1;
            bit <<= index * split;
            zOrderValue |= bit;
        }
        return zOrderValue;
    }

    private static int distance(int distance, int maxMask) {
        distance = Math.abs(distance) & maxMask;
        return Math.min(distance, maxMask + 1 - distance);
    }

    private Utils() {
    }
}
