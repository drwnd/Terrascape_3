package game.player.rendering;

import core.assets.AssetManager;
import core.rendering_api.shaders.Shader;

import game.assets.Shaders;
import game.player.Player;
import game.utils.Utils;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public static final int CHUNKS_PER_LOD = RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH;

    public RenderingOptimizer() {
        chunkVisibilityBitsBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_SHADER_STORAGE_BUFFER, chunkVisibilityBitsBuffer);
        GL46.glBufferData(GL46.GL_SHADER_STORAGE_BUFFER, (long) CHUNKS_PER_LOD * LOD_COUNT / 8, GL46.GL_DYNAMIC_DRAW);

        occluderVisibilityBitsBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_SHADER_STORAGE_BUFFER, occluderVisibilityBitsBuffer);
        GL46.glBufferData(GL46.GL_SHADER_STORAGE_BUFFER, (long) CHUNKS_PER_LOD * LOD_COUNT / 8, GL46.GL_DYNAMIC_DRAW);
    }

    public void computeVisibility(Player player) {
        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        cameraChunkX = position.x >> CHUNK_SIZE_BITS;
        cameraChunkY = position.y >> CHUNK_SIZE_BITS;
        cameraChunkZ = position.z >> CHUNK_SIZE_BITS;

        Arrays.fill(chunkVisibilityBits, -1L);
        Arrays.fill(occluderVisibilityBits, 0L);

        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod, cameraChunkX, cameraChunkY, cameraChunkZ);
        for (int lod = 0; lod < LOD_COUNT; lod++) removeEmptyChunks(lod, cameraChunkX, cameraChunkY, cameraChunkZ);
        for (int lod = 0; lod < LOD_COUNT; lod++) setOccluderVisibilityBits(lod, cameraChunkX, cameraChunkY, cameraChunkZ);

        GL46.glNamedBufferSubData(chunkVisibilityBitsBuffer, 0, chunkVisibilityBits);
        GL46.glNamedBufferSubData(occluderVisibilityBitsBuffer, 0, occluderVisibilityBits);

        Shader shader = AssetManager.get(Shaders.CULLING);
        shader.bind();
        shader.setUniform("cameraChunkPosition", cameraChunkX, cameraChunkY, cameraChunkZ);
        shader.setUniform("renderedWorldSizeBits", RENDERED_WORLD_WIDTH_BITS, RENDERED_WORLD_HEIGHT_BITS);
        shader.setUniform("longsPerLod", LONGS_PER_LOD);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, chunkVisibilityBitsBuffer);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 1, occluderVisibilityBitsBuffer);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 2, player.getMeshCollector().getOpaqueIndirectBuffer());
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 3, player.getMeshCollector().getWaterIndirectBuffer());
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 4, player.getMeshCollector().getGlassIndirectBuffer());

        GL46.glDispatchCompute(LOD_COUNT, 1, LONGS_PER_LOD);
        GL46.glMemoryBarrier(GL46.GL_ALL_BARRIER_BITS);
    }

    public void cleanUp() {
        GL46.glDeleteBuffers(chunkVisibilityBitsBuffer);
        GL46.glDeleteBuffers(occluderVisibilityBitsBuffer);
    }

    private void removeLodVisibilityOverlap(int lod, int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        lodOffset = lod * LONGS_PER_LOD;
        int lodPlayerX = cameraChunkX >> lod;
        int lodPlayerY = cameraChunkY >> lod;
        int lodPlayerZ = cameraChunkZ >> lod;

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
        int distanceX = Math.abs((cameraChunkX >> lod) - lodModelX);
        int distanceY = Math.abs((cameraChunkY >> lod) - lodModelY);
        int distanceZ = Math.abs((cameraChunkZ >> lod) - lodModelZ);

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
        int lodOffset = lod * LONGS_PER_LOD;
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

    private void removeEmptyChunks(int lod, int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        lodOffset = lod * LONGS_PER_LOD;
        int lodPlayerX = cameraChunkX >> lod;
        int lodPlayerY = cameraChunkY >> lod;
        int lodPlayerZ = cameraChunkZ >> lod;

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

    private void setOccluderVisibilityBits(int lod, int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        lodOffset = lod * LONGS_PER_LOD;
        int lodPlayerX = cameraChunkX >> lod;
        int lodPlayerY = cameraChunkY >> lod;
        int lodPlayerZ = cameraChunkZ >> lod;

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
    private int cameraChunkX, cameraChunkY, cameraChunkZ;

    private final long[] chunkVisibilityBits = new long[LOD_COUNT * LONGS_PER_LOD];
    private final long[] occluderVisibilityBits = new long[LOD_COUNT * LONGS_PER_LOD];

    private static final int LONGS_PER_LOD = CHUNKS_PER_LOD / 64;
    private final int chunkVisibilityBitsBuffer, occluderVisibilityBitsBuffer;
}
