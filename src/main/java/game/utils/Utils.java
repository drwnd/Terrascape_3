package game.utils;

import core.utils.MathUtils;
import core.utils.Vector3l;

import game.server.Game;
import game.settings.IntSettings;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Utils {

    /**
     * Calculates the index of a chunk in the rendered world array.
     * Uses the world's rendered width and mask.
     *
     * @param chunkX the x-coordinate of the chunk (Chunk Coordinates)
     * @param chunkY the y-coordinate of the chunk (Chunk Coordinates)
     * @param chunkZ the z-coordinate of the chunk (Chunk Coordinates)
     * @param lod    the Level of Detail
     * @return the index in the chunk array
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
     * Calculates the index of a chunk in a custom-sized array based on render distance.
     *
     * @param chunkX         the x-coordinate of the chunk (Chunk Coordinates)
     * @param chunkY         the y-coordinate of the chunk (Chunk Coordinates)
     * @param chunkZ         the z-coordinate of the chunk (Chunk Coordinates)
     * @param lod            the Level of Detail
     * @param renderDistance the render distance defining the array size
     * @return the index in the array
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
     * Checks if a chunk is outside the distance required to keep it loaded.
     *
     * @param cameraChunkX the x-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkY the y-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkZ the z-coordinate of the camera's chunk (Chunk Coordinates)
     * @param chunkX       the x-coordinate of the chunk to check (Chunk Coordinates)
     * @param chunkY       the y-coordinate of the chunk to check (Chunk Coordinates)
     * @param chunkZ       the z-coordinate of the chunk to check (Chunk Coordinates)
     * @param lod          the Level of Detail
     * @return true if the chunk is far enough to be unloaded
     */
    public static boolean outsideChunkKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        int renderDistance = IntSettings.RENDER_DISTANCE.value();
        return distance(chunkX - cameraChunkX, MAX_CHUNKS_MASK >> lod) > renderDistance + 1
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > renderDistance + 1
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > renderDistance + 1;
    }

    /**
     * Checks if a chunk is outside the distance required for it to be rendered.
     *
     * @param cameraChunkX the x-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkY the y-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkZ the z-coordinate of the camera's chunk (Chunk Coordinates)
     * @param chunkX       the x-coordinate of the chunk to check (Chunk Coordinates)
     * @param chunkY       the y-coordinate of the chunk to check (Chunk Coordinates)
     * @param chunkZ       the z-coordinate of the chunk to check (Chunk Coordinates)
     * @param lod          the Level of Detail
     * @return true if the chunk should not be rendered
     */
    public static boolean outsideRenderKeepDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        int renderDistance = IntSettings.RENDER_DISTANCE.value();
        return distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod) > renderDistance
                || distance(chunkZ - cameraChunkZ, MAX_CHUNKS_MASK >> lod) > renderDistance
                || distance(chunkY - cameraChunkY, MAX_CHUNKS_MASK >> lod) > renderDistance;
    }

    /**
     * Calculates the distance between the camera's chunk and another chunk in terms of chunks.
     *
     * @param cameraChunkX the x-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkY the y-coordinate of the camera's chunk (Chunk Coordinates)
     * @param cameraChunkZ the z-coordinate of the camera's chunk (Chunk Coordinates)
     * @param chunkX       the x-coordinate of the chunk (Chunk Coordinates)
     * @param chunkY       the y-coordinate of the chunk (Chunk Coordinates)
     * @param chunkZ       the z-coordinate of the chunk (Chunk Coordinates)
     * @param lod          the Level of Detail
     * @return the maximum distance along any axis in chunk units
     */
    public static long chunkDistance(long cameraChunkX, long cameraChunkY, long cameraChunkZ, long chunkX, long chunkY, long chunkZ, int lod) {
        long distanceX = distance(cameraChunkX - chunkX, MAX_CHUNKS_MASK >> lod);
        long distanceY = distance(cameraChunkY - chunkY, MAX_CHUNKS_MASK >> lod);
        long distanceZ = distance(cameraChunkZ - chunkZ, MAX_CHUNKS_MASK >> lod);

        return Math.max(distanceX, Math.max(distanceY, distanceZ));
    }


    /**
     * Wraps a chunk coordinate to be relative to a reference coordinate, accounting for world wrapping.
     *
     * @param actualPosition the chunk coordinate to wrap (Chunk Coordinates)
     * @param reference      the reference chunk coordinate (Chunk Coordinates)
     * @param lod            the Level of Detail
     * @return the wrapped chunk coordinate
     */
    public static long getWrappedChunkCoordinate(long actualPosition, long reference, int lod) {
        long maxChunks = MAX_CHUNKS_MASK + 1 >> lod;
        if (actualPosition - reference > maxChunks >>> 1) return actualPosition - maxChunks;
        if (reference - actualPosition > maxChunks >>> 1) return actualPosition + maxChunks;
        return actualPosition;
    }

    /**
     * Offsets a Vector3l by one block unit in the direction of the specified side.
     *
     * @param value the vector to offset (LOD-Specific World Block Coordinates)
     * @param side  the side to offset towards (NORTH, TOP, WEST, SOUTH, BOTTOM, or EAST)
     * @return the modified Vector3l
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
     * Calculates the component-wise minimum of two Vector3l instances, accounting for wrapping.
     *
     * @param a the first vector (LOD-Specific World Block Coordinates)
     * @param b the second vector (LOD-Specific World Block Coordinates)
     * @return a new Vector3l with the minimum components
     */
    public static Vector3l min(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMin(a.x, b.x),
                wrappedMin(a.y, b.y),
                wrappedMin(a.z, b.z)
        );
    }

    /**
     * Calculates the component-wise maximum of two Vector3l instances, accounting for wrapping.
     *
     * @param a the first vector (LOD-Specific World Block Coordinates)
     * @param b the second vector (LOD-Specific World Block Coordinates)
     * @return a new Vector3l with the maximum components
     */
    public static Vector3l max(Vector3l a, Vector3l b) {
        return new Vector3l(
                wrappedMax(a.x, b.x),
                wrappedMax(a.y, b.y),
                wrappedMax(a.z, b.z)
        );
    }

    /**
     * Sets the components of a Vector3i to the minimum of its current values and the specified coordinates.
     *
     * @param vector the vector to modify
     * @param x      the x-coordinate to compare
     * @param y      the y-coordinate to compare
     * @param z      the z-coordinate to compare
     */
    public static void min(Vector3i vector, int x, int y, int z) {
        vector.x = Math.min(vector.x, x);
        vector.y = Math.min(vector.y, y);
        vector.z = Math.min(vector.z, z);
    }

    /**
     * Sets the components of a Vector3i to the maximum of its current values and the specified coordinates.
     *
     * @param vector the vector to modify
     * @param x      the x-coordinate to compare
     * @param y      the y-coordinate to compare
     * @param z      the z-coordinate to compare
     */
    public static void max(Vector3i vector, int x, int y, int z) {
        vector.x = Math.max(vector.x, x);
        vector.y = Math.max(vector.y, y);
        vector.z = Math.max(vector.z, z);
    }

    /**
     * Generates a lookup table for Z-order curve values.
     *
     * @param size  the size of the table
     * @param split the number of bits to split
     * @param shift the amount to shift the result
     * @return an array of Z-order values
     */
    public static int[] zOrderCurveLookupTable(int size, int split, int shift) {
        int[] table = new int[size];
        for (int index = 0; index < size; index++) table[index] = zOrderCurveValue(index, split) << shift;
        return table;
    }

    /**
     * Extracts the in-chunk x-coordinate from a Z-order curve index.
     *
     * @param zCurveIndex the Z-order curve index
     * @return the in-chunk x-coordinate [0, 63] (In-Chunk Block Coordinates)
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
     * Extracts the in-chunk y-coordinate from a Z-order curve index.
     *
     * @param zCurveIndex the Z-order curve index
     * @return the in-chunk y-coordinate [0, 63] (In-Chunk Block Coordinates)
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
     * Extracts the in-chunk z-coordinate from a Z-order curve index.
     *
     * @param zCurveIndex the Z-order curve index
     * @return the in-chunk z-coordinate [0, 63] (In-Chunk Block Coordinates)
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
     * Calculates a single component's Z-order curve value.
     *
     * @param value the component value
     * @param split the bit splitting factor
     * @return the Z-order value for this component
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
     * Calculates the absolute distance between two coordinates on a wrapped axis.
     *
     * @param distance the signed distance
     * @param maxMask  the mask representing the axis size
     * @return the shortest distance accounting for wrapping
     */
    private static long distance(long distance, long maxMask) {
        distance = Math.abs(distance) & maxMask;
        return Math.min(distance, maxMask + 1 - distance);
    }

    /**
     * Calculates the minimum value between two numbers, accounting for wrapping at Long.MAX_VALUE.
     *
     * @param a the first number
     * @param b the second number
     * @return the minimum number
     */
    public static long wrappedMin(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.max(a, b);
        return Math.min(a, b);
    }

    /**
     * Calculates the maximum value between two numbers, accounting for wrapping at Long.MAX_VALUE.
     *
     * @param a the first number
     * @param b the second number
     * @return the maximum number
     */
    public static long wrappedMax(long a, long b) {
        if (Math.abs((float) a - b) > Long.MAX_VALUE) return Math.min(a, b);
        return Math.max(a, b);
    }


    /**
     * Sanitizes a file name by replacing disallowed characters with underscores.
     *
     * @param fileName the file name to sanitize
     * @return the sanitized file name
     */
    public static String sanitizeFileName(String fileName) {
        char[] chars = fileName.strip().toCharArray();
        for (int index = 0; index < chars.length; index++) if (!isAllowedChar(chars[index])) chars[index] = '_';
        return String.valueOf(chars);
    }

    /**
     * Checks if a character is allowed in a file name.
     *
     * @param character the character to check
     * @return true if the character is a digit, letter, underscore, hyphen, or space
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
