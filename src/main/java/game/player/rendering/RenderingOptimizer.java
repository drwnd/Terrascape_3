package game.player.rendering;

import core.utils.IntArrayList;
import game.player.Player;
import game.server.Game;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public static final int INDIRECT_COMMAND_SIZE = 16;

    public RenderingOptimizer() {
        opaqueIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, opaqueIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * 6, GL46.GL_DYNAMIC_DRAW);

        waterIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, waterIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL46.GL_DYNAMIC_DRAW);

        glassIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, glassIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL46.GL_DYNAMIC_DRAW);


    }

    public void computeVisibility(Player player) {
        Matrix4f projectionViewMatrix = Transformation.getFrustumCullingMatrix(player.getCamera());
        FrustumIntersection frustumIntersection = new FrustumIntersection(projectionViewMatrix);

        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        cameraChunkX = position.x >> CHUNK_SIZE_BITS;
        cameraChunkY = position.y >> CHUNK_SIZE_BITS;
        cameraChunkZ = position.z >> CHUNK_SIZE_BITS;
        playerX = position.x;
        playerY = position.y;
        playerZ = position.z;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod);

        generateIndirectCommands();
        GL46.glNamedBufferSubData(opaqueIndirectBuffer, 0, opaqueCommands.toArray());
        GL46.glNamedBufferSubData(waterIndirectBuffer, 0, waterCommands.toArray());
        GL46.glNamedBufferSubData(glassIndirectBuffer, 0, glassCommands.toArray());
    }

    public long getOpaqueLodStart(int lod) {
        return lodStarts[lod * 2];
    }

    public int getOpaqueLodDrawCount(int lod) {
        return lodDrawCounts[lod * 2];
    }

    public long getTransparentLodStart(int lod) {
        return lodStarts[lod * 2 + 1];
    }

    public int getTransparentLodDrawCount(int lod) {
        return lodDrawCounts[lod * 2 + 1];
    }

    public int getOpaqueIndirectBuffer() {
        return opaqueIndirectBuffer;
    }

    public int getWaterIndirectBuffer() {
        return waterIndirectBuffer;
    }

    public int getGlassIndirectBuffer() {
        return glassIndirectBuffer;
    }


    private void computeLodVisibility(int lod, FrustumIntersection frustumIntersection, long[][] visibilityBits) {
        int chunkX = cameraChunkX >> lod;
        int chunkY = cameraChunkY >> lod;
        int chunkZ = cameraChunkZ >> lod;

        lodVisibilityBits = visibilityBits[lod];
        Arrays.fill(lodVisibilityBits, 0L);

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        lodVisibilityBits[chunkIndex >> 6] |= 1L << chunkIndex;

        fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (1 << NORTH), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (1 << SOUTH), lod, frustumIntersection);

        fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (1 << TOP), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (1 << BOTTOM), lod, frustumIntersection);

        fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (1 << WEST), lod, frustumIntersection);
        fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (1 << EAST), lod, frustumIntersection);
    }

    private void fillVisibleChunks(int chunkX, int chunkY, int chunkZ, byte traveledDirections, int lod, FrustumIntersection intersection) {
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        if ((lodVisibilityBits[chunkIndex >> 6] & 1L << chunkIndex) != 0) return;

        if (Game.getWorld().getChunk(chunkIndex, lod) == null) return;
        if (!intersection.testAab(
                (chunkX << chunkSizeBits) - playerX,
                (chunkY << chunkSizeBits) - playerY,
                (chunkZ << chunkSizeBits) - playerZ,
                (chunkX + 1 << chunkSizeBits) - playerX,
                (chunkY + 1 << chunkSizeBits) - playerY,
                (chunkZ + 1 << chunkSizeBits) - playerZ)) return;

        lodVisibilityBits[chunkIndex >> 6] |= 1L << chunkIndex;

        if ((traveledDirections & 1 << SOUTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (traveledDirections | 1 << NORTH), lod, intersection);
        if ((traveledDirections & 1 << NORTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (traveledDirections | 1 << SOUTH), lod, intersection);

        if ((traveledDirections & 1 << BOTTOM) == 0)
            fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (traveledDirections | 1 << TOP), lod, intersection);
        if ((traveledDirections & 1 << TOP) == 0)
            fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (traveledDirections | 1 << BOTTOM), lod, intersection);

        if ((traveledDirections & 1 << EAST) == 0)
            fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << WEST), lod, intersection);
        if ((traveledDirections & 1 << WEST) == 0)
            fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << EAST), lod, intersection);
    }

    private void removeLodVisibilityOverlap(int lod) {
        lodVisibilityBits = visibilityBits[lod];
        int lodCameraChunkX = cameraChunkX >> lod;
        int lodCameraChunkY = cameraChunkY >> lod;
        int lodCameraChunkZ = cameraChunkZ >> lod;

        int endX = Utils.makeOdd(lodCameraChunkX + RENDER_DISTANCE_XZ + 2);
        int endY = Utils.makeOdd(lodCameraChunkY + RENDER_DISTANCE_Y + 2);
        int endZ = Utils.makeOdd(lodCameraChunkZ + RENDER_DISTANCE_XZ + 2);

        int startX = Utils.makeEven(lodCameraChunkX - RENDER_DISTANCE_XZ - 2);
        int startY = Utils.makeEven(lodCameraChunkY - RENDER_DISTANCE_Y - 2);
        int startZ = Utils.makeEven(lodCameraChunkZ - RENDER_DISTANCE_XZ - 2);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++)
                    removeModelVisibilityOverlap(lod, lodModelX, lodModelY, lodModelZ);
    }

    private void removeModelVisibilityOverlap(int lod, int lodModelX, int lodModelY, int lodModelZ) {
        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) return;

        OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
        TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
        if (opaqueModel == null || transparentModel == null) {
            lodVisibilityBits[index >> 6] &= ~(1L << index);
            return;
        }

        if (lod == 0 || modelFarEnoughAway(lodModelX, lodModelY, lodModelZ, lod)) return;

        int nextLodX = lodModelX << 1;
        int nextLodY = lodModelY << 1;
        int nextLodZ = lodModelZ << 1;
        if (modelCubePresent(nextLodX, nextLodY, nextLodZ, lod - 1)) lodVisibilityBits[index >> 6] &= ~(1L << index);
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
        long[] visibilityBits = this.visibilityBits[lod];
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ + 1, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
    }

    private void generateIndirectCommands() {
        opaqueCommands.clear();
        waterCommands.clear();
        glassCommands.clear();

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int drawCount = 0;
            lodStarts[lod * 2 + 0] = (long) opaqueCommands.size() / 4 * INDIRECT_COMMAND_SIZE;
            lodStarts[lod * 2 + 1] = (long) waterCommands.size() / 4 * INDIRECT_COMMAND_SIZE;

            lodVisibilityBits = visibilityBits[lod];
            int lodCameraChunkX = cameraChunkX >> lod;
            int lodCameraChunkY = cameraChunkY >> lod;
            int lodCameraChunkZ = cameraChunkZ >> lod;

            int endX = Utils.makeOdd(lodCameraChunkX + RENDER_DISTANCE_XZ + 2);
            int endY = Utils.makeOdd(lodCameraChunkY + RENDER_DISTANCE_Y + 2);
            int endZ = Utils.makeOdd(lodCameraChunkZ + RENDER_DISTANCE_XZ + 2);

            int startX = Utils.makeEven(lodCameraChunkX - RENDER_DISTANCE_XZ - 2);
            int startY = Utils.makeEven(lodCameraChunkY - RENDER_DISTANCE_Y - 2);
            int startZ = Utils.makeEven(lodCameraChunkZ - RENDER_DISTANCE_XZ - 2);

            for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
                for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                    for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {

                        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                        if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;

                        OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
                        TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
                        if (opaqueModel == null || transparentModel == null) continue;
                        if (opaqueModel.isEmpty() && transparentModel.isEmpty()) continue;

                        drawCount++;
                        opaqueModel.addData(opaqueCommands, lodCameraChunkX, lodCameraChunkY, lodCameraChunkZ);
                        transparentModel.addData(waterCommands, glassCommands);
                    }
            lodDrawCounts[lod * 2 + 0] = drawCount * 6;
            lodDrawCounts[lod * 2 + 1] = drawCount;
        }
    }


    private long[] lodVisibilityBits;
    private MeshCollector meshCollector;
    private int cameraChunkX, cameraChunkY, cameraChunkZ;
    private int playerX, playerY, playerZ;

    private final int opaqueIndirectBuffer, waterIndirectBuffer, glassIndirectBuffer;
    private final long[][] visibilityBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
    //    private final long[][] occludedBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
    private final long[] lodStarts = new long[LOD_COUNT * 2];
    private final int[] lodDrawCounts = new int[LOD_COUNT * 2];
    private final IntArrayList opaqueCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 256);
    private final IntArrayList waterCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 128);
    private final IntArrayList glassCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 128);
}
