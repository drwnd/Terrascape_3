package game.utils;

import core.utils.MathUtils;
import core.utils.Vector3l;

import game.server.Game;
import game.settings.IntSettings;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Utils {

/**
 * Returns the chunk index.
 *
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @return result
 */
    public static int getChunkIndex(long chunkX, long chunkY, long chunkZ, int lod) {
        int widthMask = Game.getWorld().RENDERED_WORLD_WIDTH_MASK;
        int widthBits = Game.getWorld().RENDERED_WORLD_WIDTH_BITS;

        chunkX &= widthMask & MAX_CHUNKS_MASK >> lod;
        chunkY &= widthMask & MAX_CHUNKS_MASK >> lod;
        chunkZ &= widthMask & MAX_CHUNKS_MASK >> lod;

        return (int) (((chunkX << widthBits) + chunkZ << widthBits) + chunkY);
    }

/**
 * Returns the chunk index.
 *
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @param renderDistance parameter
 * @return result
 */
    public static int getChunkIndex(long chunkX, long chunkY, long chunkZ, int lod, int renderDistance) {
        int widthMask = MathUtils.nextLargestPowOf2(renderDistance * 2 + 3) - 1;
        int widthBits = Integer.numberOfTrailingZeros(widthMask + 1);

        chunkX &= widthMask & MAX_CHUNKS_MASK >> lod;
        chunkY &= widthMask & MAX_CHUNKS_MASK >> lod;
        chunkZ &= widthMask & MAX_CHUNKS_MASK >> lod;

        return (int) (((chunkX << widthBits) + chunkZ << widthBits) + chunkY);
    }

/**
 * Performs outside chunk keep distance.
 *
 * @param cameraChunkX X coordinate in local block coordinates
 * @param cameraChunkY Y coordinate in local block coordinates
 * @param cameraChunkZ Z coordinate in local block coordinates
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @return true if the condition holds
 */
    public static boolean outsideChunkKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        int renderDistance = IntSettings.RENDER_DISTANCE.value();
        return distance(chunkX - cameraChunkX, MAX_CHUNKS_MASK >> lod) > renderDistance + 1
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > renderDistance + 1
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > renderDistance + 1;
    }

/**
 * Performs outside render keep distance.
 *
 * @param cameraChunkX X coordinate in local block coordinates
 * @param cameraChunkY Y coordinate in local block coordinates
 * @param cameraChunkZ Z coordinate in local block coordinates
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @return true if the condition holds
 */
    public static boolean outsideRenderKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        int renderDistance = IntSettings.RENDER_DISTANCE.value();
        return distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod) > renderDistance
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > renderDistance
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > renderDistance;
    }

/**
 * Performs chunk distance.
 *
 * @param cameraChunkX X coordinate in local block coordinates
 * @param cameraChunkY Y coordinate in local block coordinates
 * @param cameraChunkZ Z coordinate in local block coordinates
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @return result
 */
    public static long chunkDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        long distanceX = distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod);
        long distanceY = distance(cameraChunkY - chunkY, MAX_CHUNKS_MASK >> lod);
        long distanceZ = distance(cameraChunkZ - chunkZ, MAX_CHUNKS_MASK >> lod);

        return Math.max(distanceX, Math.max(distanceY, distanceZ));
    }


/**
 * Returns the wrapped chunk coordinate.
 *
 * @param actualPosition parameter
 * @param reference parameter
 * @param lod parameter
 * @return result
 */
    public static long getWrappedChunkCoordinate(long actualPosition, long reference, int lod) {
        long maxChunks = MAX_CHUNKS_MASK + 1 >> lod;
        if (actualPosition - reference > maxChunks >>> 1) return actualPosition - maxChunks;
        if (reference - actualPosition > maxChunks >>> 1) return actualPosition + maxChunks;
        return actualPosition;
    }

/**
 * Performs offset by normal.
 *
 * @param value parameter
 * @param side parameter
 * @return result
 */
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

/**
 * Performs min.
 *
 * @param a parameter
 * @param b parameter
 * @return result
 */
    public static Vector3l min(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMin(a.x, b.x),
                wrappedMin(a.y, b.y),
                wrappedMin(a.z, b.z)
        );
    }

/**
 * Performs max.
 *
 * @param a parameter
 * @param b parameter
 * @return result
 */
    public static Vector3l max(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMax(a.x, b.x),
                wrappedMax(a.y, b.y),
                wrappedMax(a.z, b.z)
        );
    }

/**
 * Performs min.
 *
 * @param vector 3D vector in local block coordinates
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 */
    public static void min(Vector3i vector, int x, int y, int z) {
        vector.x = Math.min(vector.x, x);
        vector.y = Math.min(vector.y, y);
        vector.z = Math.min(vector.z, z);
    }

/**
 * Performs max.
 *
 * @param vector 3D vector in local block coordinates
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 */
    public static void max(Vector3i vector, int x, int y, int z) {
        vector.x = Math.max(vector.x, x);
        vector.y = Math.max(vector.y, y);
        vector.z = Math.max(vector.z, z);
    }

/**
 * Performs z order curve lookup table.
 *
 * @param size parameter
 * @param split parameter
 * @param shift parameter
 * @return array result
 */
    public static int[] zOrderCurveLookupTable(int size, int split, int shift) {
        int[] table = new int[size];
        for (int index = 0; index < size; index++) table[index] = zOrderCurveValue(index, split) << shift;
        return table;
    }

/**
 * Returns the in chunk x.
 *
 * @param zCurveIndex X coordinate in local block coordinates
 * @return result
 */
    public static int getInChunkX(int zCurveIndex) {
        return zCurveIndex >> 2 & 1 |
                zCurveIndex >> 4 & 2 |
                zCurveIndex >> 6 & 4 |
                zCurveIndex >> 8 & 8 |
                zCurveIndex >> 10 & 16 |
                zCurveIndex >> 12 & 32;
    }

/**
 * Returns the in chunk y.
 *
 * @param zCurveIndex X coordinate in local block coordinates
 * @return result
 */
    public static int getInChunkY(int zCurveIndex) {
        return zCurveIndex >> 1 & 1 |
                zCurveIndex >> 3 & 2 |
                zCurveIndex >> 5 & 4 |
                zCurveIndex >> 7 & 8 |
                zCurveIndex >> 9 & 16 |
                zCurveIndex >> 11 & 32;
    }

/**
 * Returns the in chunk z.
 *
 * @param zCurveIndex X coordinate in local block coordinates
 * @return result
 */
    public static int getInChunkZ(int zCurveIndex) {
        return zCurveIndex & 1 |
                zCurveIndex >> 2 & 2 |
                zCurveIndex >> 4 & 4 |
                zCurveIndex >> 6 & 8 |
                zCurveIndex >> 8 & 16 |
                zCurveIndex >> 10 & 32;
    }


/**
 * Performs z order curve value.
 *
 * @param value parameter
 * @param split parameter
 * @return result
 */
    private static int zOrderCurveValue(int value, int split) {
        int zOrderValue = 0;
        for (int index = 0; index < 10; index++) {
            int bit = value >> index & 1;
            bit <<= index * split;
            zOrderValue |= bit;
        }
        return zOrderValue;
    }

/**
 * Performs distance.
 *
 * @param distance parameter
 * @param maxMask parameter
 * @return result
 */
    private static long distance(long distance, long maxMask) {
        distance = Math.abs(distance) & maxMask;
        return Math.min(distance, maxMask + 1 - distance);
    }

/**
 * Performs wrapped min.
 *
 * @param a parameter
 * @param b parameter
 * @return result
 */
    public static long wrappedMin(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.max(a, b);
        return Math.min(a, b);
    }

/**
 * Performs wrapped max.
 *
 * @param a parameter
 * @param b parameter
 * @return result
 */
    public static long wrappedMax(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.min(a, b);
        return Math.max(a, b);
    }


/**
 * Performs sanitize file name.
 *
 * @param fileName parameter
 * @return result
 */
    public static String sanitizeFileName(String fileName) {
        char[] chars = fileName.strip().toCharArray();
        for (int index = 0; index < chars.length; index++) if (!isAllowedChar(chars[index])) chars[index] = '_';
        return String.valueOf(chars);
    }

/**
 * Checks whether allowed char.
 *
 * @param character parameter
 * @return true if the condition holds
 */
    private static boolean isAllowedChar(char character) {
        if (character >= '0' && character <= '9') return true;
        if (character >= 'a' && character <= 'z') return true;
        if (character >= 'A' && character <= 'Z') return true;
        return character == '_' || character == '-' || character == ' ';
    }

    private Utils() {
    }
}
