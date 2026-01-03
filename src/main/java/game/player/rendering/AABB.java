package game.player.rendering;

import core.utils.IntArrayList;

import static game.utils.Constants.*;

public final class AABB {

    public int minX, minY, minZ;
    public int maxX, maxY, maxZ;

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


    public void addData(IntArrayList aabbs, int totalX, int totalY, int totalZ, int lod) {
        aabbs.add(totalX + minX);
        aabbs.add(totalY + minY);
        aabbs.add(totalZ + minZ);

        aabbs.add(lod << 21 | Math.max(0, maxX - minX) << 14 | Math.max(0, maxY - minY) << 7 | Math.max(0, maxZ - minZ));
    }

    public boolean intersects(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.minX < maxX && minX < this.maxX
                && this.minY < maxY && minY < this.maxY
                && this.minZ < maxZ && minZ < this.maxZ;
    }

    public boolean includes(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.minX <= minX && maxX <= this.maxX
                && this.minY <= minY && maxY <= this.maxY
                && this.minZ <= minZ && maxZ <= this.maxZ;
    }

    public void min(int x, int y, int z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
    }

    public void max(int x, int y, int z) {
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }

    public void excludeMaximizeSurfaceArea(int x, int y, int z, int size) {
        int removePosX = halfSurfaceArea(x + size - minX, maxY - minY, maxZ - minZ);
        int removeNegX = halfSurfaceArea(maxX - x, maxY - minY, maxZ - minZ);
        int removePosY = halfSurfaceArea(maxX - minX, y + size - minY, maxZ - minZ);
        int removeNegY = halfSurfaceArea(maxX - minX, maxY - y, maxZ - minZ);
        int removePosZ = halfSurfaceArea(maxX - minX, maxY - minY, z + size - minZ);
        int removeNegZ = halfSurfaceArea(maxX - minX, maxY - minY, maxZ - z);

        int max = Math.max(removePosX, Math.max(removeNegX, Math.max(removePosY, Math.max(removeNegY, Math.max(removePosZ, removeNegZ)))));

        if (max == removePosY) maxY = y;
        else if (max == removeNegY) minY = y + size;
        else if (max == removePosX) maxX = x;
        else if (max == removeNegX) minX = x + size;
        else if (max == removePosZ) maxZ = z;
        else minZ = z + size;
    }

    public void setEmpty() {
        maxX = maxY = maxZ = -1;
        minX = minY = minZ = 0;
    }


    private static int halfSurfaceArea(int lengthX, int lengthY, int lengthZ) {
        if (lengthX < 0 || lengthY < 0 || lengthZ < 0) return 0;
        return lengthX * lengthY + lengthX * lengthZ + lengthY * lengthZ;
    }
}
