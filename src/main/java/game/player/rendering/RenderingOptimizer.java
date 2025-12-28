package game.player.rendering;

import game.player.Player;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public static final int LOD_OFFSET_SIZE = RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH / 64;

    public RenderingOptimizer() {

    }

    public void computeVisibility(Player player) {
        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        playerChunkX = position.x >> CHUNK_SIZE_BITS;
        playerChunkY = position.y >> CHUNK_SIZE_BITS;
        playerChunkZ = position.z >> CHUNK_SIZE_BITS;

        Arrays.fill(chunkVisibilityBits, -1L);
        Arrays.fill(occluderVisibilityBits, 0L);

        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod, playerChunkX, playerChunkY, playerChunkZ);
        for (int lod = 0; lod < LOD_COUNT; lod++) removeEmptyChunks(lod, playerChunkX, playerChunkY, playerChunkZ);
        for (int lod = 0; lod < LOD_COUNT; lod++) setOccluderVisibilityBits(lod, playerChunkX, playerChunkY, playerChunkZ);
    }

    public long[] getChunkVisibilityBits() {
        return chunkVisibilityBits;
    }

    public long[] getOccluderVisibilityBits() {
        return occluderVisibilityBits;
    }

    private void removeLodVisibilityOverlap(int lod, int playerChunkX, int playerChunkY, int playerChunkZ) {
        lodOffset = lod * LOD_OFFSET_SIZE;
        int lodPlayerX = playerChunkX >> lod;
        int lodPlayerY = playerChunkY >> lod;
        int lodPlayerZ = playerChunkZ >> lod;

        int endX = Utils.makeOdd(lodPlayerX + RENDER_DISTANCE_XZ + 2);
        int endY = Utils.makeOdd(lodPlayerY + RENDER_DISTANCE_Y + 2);
        int endZ = Utils.makeOdd(lodPlayerZ + RENDER_DISTANCE_XZ + 2);

        int startX = Utils.makeEven(lodPlayerX - RENDER_DISTANCE_XZ - 2);
        int startY = Utils.makeEven(lodPlayerY - RENDER_DISTANCE_Y - 2);
        int startZ = Utils.makeEven(lodPlayerZ - RENDER_DISTANCE_XZ - 2);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++)
                    removeModelVisibilityOverlap(lod, lodModelX, lodModelY, lodModelZ);
    }

    private void removeModelVisibilityOverlap(int lod, int lodModelX, int lodModelY, int lodModelZ) {
        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) == 0) return;

        OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
        TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
        if (opaqueModel == null || transparentModel == null) {
            chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(1L << index);
            return;
        }

        if (lod == 0 || modelFarEnoughAway(lodModelX, lodModelY, lodModelZ, lod)) return;

        int nextLodX = lodModelX << 1;
        int nextLodY = lodModelY << 1;
        int nextLodZ = lodModelZ << 1;
        if (modelCubePresent(nextLodX, nextLodY, nextLodZ, lod - 1)) chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(1L << index);
        else clearModelCubeVisibility(nextLodX, nextLodY, nextLodZ, lod - 1);
    }

    private boolean modelFarEnoughAway(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        int distanceX = Math.abs((playerChunkX >> lod) - lodModelX);
        int distanceY = Math.abs((playerChunkY >> lod) - lodModelY);
        int distanceZ = Math.abs((playerChunkZ >> lod) - lodModelZ);

        return distanceX > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceZ > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceY > (RENDER_DISTANCE_Y >> 1) + 1;
    }

    private boolean modelCubePresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        return meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ + 1, lod);
    }

    private void clearModelCubeVisibility(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        int lodOffset = lod * LOD_OFFSET_SIZE;
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1, lod);
        chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ, lod);
        chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ + 1, lod);
        chunkVisibilityBits[lodOffset + (index >> 6)] &= ~(3L << index);
    }

    private void removeEmptyChunks(int lod, int playerChunkX, int playerChunkY, int playerChunkZ) {
        lodOffset = lod * LOD_OFFSET_SIZE;
        int lodPlayerX = playerChunkX >> lod;
        int lodPlayerY = playerChunkY >> lod;
        int lodPlayerZ = playerChunkZ >> lod;

        int endX = Utils.makeOdd(lodPlayerX + RENDER_DISTANCE_XZ + 2);
        int endY = Utils.makeOdd(lodPlayerY + RENDER_DISTANCE_Y + 2);
        int endZ = Utils.makeOdd(lodPlayerZ + RENDER_DISTANCE_XZ + 2);

        int startX = Utils.makeEven(lodPlayerX - RENDER_DISTANCE_XZ - 2);
        int startY = Utils.makeEven(lodPlayerY - RENDER_DISTANCE_Y - 2);
        int startZ = Utils.makeEven(lodPlayerZ - RENDER_DISTANCE_XZ - 2);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    if (meshCollector.isNonEmptyModelPresent(lodModelX, lodModelY, lodModelZ, lod)) continue;
                    int chunkIndex = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    chunkVisibilityBits[lodOffset + (chunkIndex >> 6)] &= ~(1L << chunkIndex);
                }
    }

    private void setOccluderVisibilityBits(int lod, int playerChunkX, int playerChunkY, int playerChunkZ) {
        lodOffset = lod * LOD_OFFSET_SIZE;
        int lodPlayerX = playerChunkX >> lod;
        int lodPlayerY = playerChunkY >> lod;
        int lodPlayerZ = playerChunkZ >> lod;

        int endX = Utils.makeOdd(lodPlayerX + RENDER_DISTANCE_XZ + 2);
        int endY = Utils.makeOdd(lodPlayerY + RENDER_DISTANCE_Y + 2);
        int endZ = Utils.makeOdd(lodPlayerZ + RENDER_DISTANCE_XZ + 2);

        int startX = Utils.makeEven(lodPlayerX - RENDER_DISTANCE_XZ - 2);
        int startY = Utils.makeEven(lodPlayerY - RENDER_DISTANCE_Y - 2);
        int startZ = Utils.makeEven(lodPlayerZ - RENDER_DISTANCE_XZ - 2);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    if (!isNeighborBitSet(lod, lodModelX, lodModelY, lodModelZ)) continue;
                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    occluderVisibilityBits[lodOffset + (index >> 6)] |= 1L << index;
                }
    }

    private boolean isNeighborBitSet(int lod, int lodModelX, int lodModelY, int lodModelZ) {
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ - 1, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0) return true;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0) return true;
        index = Utils.getChunkIndex(lodModelX, lodModelY - 1, lodModelZ, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0) return true;
        index = Utils.getChunkIndex(lodModelX, lodModelY + 1, lodModelZ, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0) return true;
        index = Utils.getChunkIndex(lodModelX - 1, lodModelY, lodModelZ, lod);
        if ((chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0) return true;
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ, lod);
        return (chunkVisibilityBits[lodOffset + (index >> 6)] & 1L << index) != 0;
    }

    private int lodOffset;
    private MeshCollector meshCollector;
    private int playerChunkX, playerChunkY, playerChunkZ;

    private final long[] chunkVisibilityBits = new long[LOD_COUNT * LOD_OFFSET_SIZE];
    private final long[] occluderVisibilityBits = new long[LOD_COUNT * LOD_OFFSET_SIZE];
}
