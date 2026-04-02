package game.utils;

import core.utils.Vector3l;

import static game.utils.Constants.*;

public final class Utils {

    public static int getChunkIndex(long chunkX, long chunkY, long chunkZ, int lod) {
        chunkX &= RENDERED_WORLD_WIDTH_MASK & MAX_CHUNKS_MASK >> lod;
        chunkY &= RENDERED_WORLD_HEIGHT_MASK & MAX_CHUNKS_MASK >> lod;
        chunkZ &= RENDERED_WORLD_WIDTH_MASK & MAX_CHUNKS_MASK >> lod;

        return (int) (((chunkX << RENDERED_WORLD_WIDTH_BITS) + chunkZ << RENDERED_WORLD_HEIGHT_BITS) + chunkY);
    }

    public static boolean outsideChunkKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        return distance(chunkX - cameraChunkX, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_XZ + 1
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_XZ + 1
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_Y + 1;
    }

    public static boolean outsideRenderKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        return distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_XZ
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_XZ
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > RENDER_DISTANCE_Y;
    }

    public static long chunkDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        long distanceX = distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod);
        long distanceY = distance(cameraChunkY - chunkY, MAX_CHUNKS_MASK >> lod);
        long distanceZ = distance(cameraChunkZ - chunkZ, MAX_CHUNKS_MASK >> lod);

        return Math.max(distanceX, Math.max(distanceY, distanceZ));
    }


    public static long getWrappedChunkCoordinate(long actualPosition, long reference, int lod) {
        long maxChunks = MAX_CHUNKS_MASK + 1 >> lod;
        if (actualPosition - reference > maxChunks >>> 1) return actualPosition - maxChunks;
        if (reference - actualPosition > maxChunks >>> 1) return actualPosition + maxChunks;
        return actualPosition;
    }

    public static Vector3l offsetByNormal(Vector3l value, int side) {
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

    public static Vector3l min(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMin(a.x, b.x),
                wrappedMin(a.y, b.y),
                wrappedMin(a.z, b.z)
        );
    }

    public static Vector3l max(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMax(a.x, b.x),
                wrappedMax(a.y, b.y),
                wrappedMax(a.z, b.z)
        );
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

    private static long distance(long distance, long maxMask) {
        distance = Math.abs(distance) & maxMask;
        return Math.min(distance, maxMask + 1 - distance);
    }

    public static long wrappedMin(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.max(a, b);
        return Math.min(a, b);
    }

    public static long wrappedMax(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.min(a, b);
        return Math.max(a, b);
    }

    private Utils() {
    }
}
