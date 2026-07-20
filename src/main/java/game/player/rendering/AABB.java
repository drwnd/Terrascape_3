package game.player.rendering;

import core.utils.IntArrayList;

import static game.utils.Constants.*;

public final class AABB {

    public int minX, minY, minZ;
    public int maxX, maxY, maxZ;

/**
 * Creates a new AABB instance.
 *
 * @param minX X coordinate in local block coordinates
 * @param minY Y coordinate in local block coordinates
 * @param minZ Z coordinate in local block coordinates
 * @param maxX X coordinate in local block coordinates
 * @param maxY Y coordinate in local block coordinates
 * @param maxZ Z coordinate in local block coordinates
 */
    public AABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static AABB newMinChunkAABB() {
        return new AABB(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE, 0, 0, 0);
    }

    public static AABB newMaxChunkAABB() {
        return new AABB(0, 0, 0, CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);
    }


/**
 * Adds data.
 *
 * @param aabbs parameter
 * @param totalX X coordinate in local block coordinates
 * @param totalY Y coordinate in local block coordinates
 * @param totalZ Z coordinate in local block coordinates
 * @param lod parameter
 */
    public void addData(IntArrayList aabbs, int totalX, int totalY, int totalZ, int lod) {
        aabbs.add(totalX + (minX << lod));
        aabbs.add(totalY + (minY << lod));
        aabbs.add(totalZ + (minZ << lod));

        aabbs.add(lod << 21 | Math.max(0, maxX - minX) << 14 | Math.max(0, maxY - minY) << 7 | Math.max(0, maxZ - minZ));
    }

/**
 * Performs intersects.
 *
 * @param minX X coordinate in local block coordinates
 * @param minY Y coordinate in local block coordinates
 * @param minZ Z coordinate in local block coordinates
 * @param maxX X coordinate in local block coordinates
 * @param maxY Y coordinate in local block coordinates
 * @param maxZ Z coordinate in local block coordinates
 * @return true if the condition holds
 */
    public boolean intersects(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.minX < maxX && minX < this.maxX
                && this.minY < maxY && minY < this.maxY
                && this.minZ < maxZ && minZ < this.maxZ;
    }

/**
 * Performs includes.
 *
 * @param minX X coordinate in local block coordinates
 * @param minY Y coordinate in local block coordinates
 * @param minZ Z coordinate in local block coordinates
 * @param maxX X coordinate in local block coordinates
 * @param maxY Y coordinate in local block coordinates
 * @param maxZ Z coordinate in local block coordinates
 * @return true if the condition holds
 */
    public boolean includes(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.minX <= minX && maxX <= this.maxX
                && this.minY <= minY && maxY <= this.maxY
                && this.minZ <= minZ && maxZ <= this.maxZ;
    }

    public boolean isEmpty() {
        return maxX <= minX || maxY <= minY || maxZ <= minZ;
    }

    public boolean isMaxChunk() {
        return minX == 0 && minY == 0 && minZ == 0 && maxX == CHUNK_SIZE && maxY == CHUNK_SIZE && maxZ == CHUNK_SIZE;
    }

/**
 * Performs min.
 *
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 */
    public void min(int x, int y, int z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
    }

/**
 * Performs max.
 *
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 */
    public void max(int x, int y, int z) {
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }

/**
 * Performs exclude maximize surface area.
 *
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 * @param size parameter
 */
    public void excludeMaximizeSurfaceArea(int x, int y, int z, int size) {
        int removePosX = halfSurfaceArea(x - minX, maxY - minY, maxZ - minZ);
        int removeNegX = halfSurfaceArea(maxX - x - size, maxY - minY, maxZ - minZ);
        int removePosY = halfSurfaceArea(maxX - minX, y - minY, maxZ - minZ);
        int removeNegY = halfSurfaceArea(maxX - minX, maxY - y - size, maxZ - minZ);
        int removePosZ = halfSurfaceArea(maxX - minX, maxY - minY, z - minZ);
        int removeNegZ = halfSurfaceArea(maxX - minX, maxY - minY, maxZ - z - size);

        int max = Math.max(removePosX, Math.max(removeNegX, Math.max(removePosY, Math.max(removeNegY, Math.max(removePosZ, removeNegZ)))));

        if (max == removePosY) maxY = y;
        else if (max == removeNegY) minY = y + size;
        else if (max == removePosX) maxX = x;
        else if (max == removeNegX) minX = x + size;
        else if (max == removePosZ) maxZ = z;
        else minZ = z + size;
    }

/**
 * Performs set.
 *
 * @param minX X coordinate in local block coordinates
 * @param minY Y coordinate in local block coordinates
 * @param minZ Z coordinate in local block coordinates
 * @param maxX X coordinate in local block coordinates
 * @param maxY Y coordinate in local block coordinates
 * @param maxZ Z coordinate in local block coordinates
 */
    public void set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

/**
 * Sets empty.
 */
    public void setEmpty() {
        maxX = maxY = maxZ = -1;
        minX = minY = minZ = 0;
    }

    public int getHalfSurfaceArea() {
        return halfSurfaceArea(maxX - minX, maxY - minY, maxZ - minZ);
    }


/**
 * Performs half surface area.
 *
 * @param lengthX extent along the X axis in local block coordinates
 * @param lengthY extent along the Y axis in local block coordinates
 * @param lengthZ extent along the Z axis in local block coordinates
 * @return result
 */
    private static int halfSurfaceArea(int lengthX, int lengthY, int lengthZ) {
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return 0;
        return lengthX * lengthY + lengthX * lengthZ + lengthY * lengthZ;
    }
}
