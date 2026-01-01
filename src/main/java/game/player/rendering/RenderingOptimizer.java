package game.player.rendering;

import game.player.Player;
import game.server.Game;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public RenderingOptimizer() {

    }

    public void computeVisibility(Player player, Matrix4f projectionViewMatrix) {
        FrustumIntersection frustumIntersection = new FrustumIntersection(Transformation.getFrustumCullingMatrix(player.getCamera()));
        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        cameraX = position.x;
        cameraY = position.y;
        cameraZ = position.z;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod);

//        long start = System.nanoTime(); // TODO remove

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccluders(lod);
        rasterizer.set(projectionViewMatrix, cameraX, cameraY, cameraZ);
        rasterizer.renderOccluders(aabbs);

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccludees(lod);
        rasterizer.testOccludees(aabbs, visibilityBits);

//        System.out.println(System.nanoTime() - start); // TODO remove
    }

    public long[][] getVisibilityBits() {
        return visibilityBits;
    }

    public float[] getDepthMap() {
        return rasterizer.getDepthMap();
    }


    private void computeLodVisibility(int lod, FrustumIntersection frustumIntersection, long[][] visibilityBits) {
        int chunkX = cameraX >> CHUNK_SIZE_BITS + lod;
        int chunkY = cameraY >> CHUNK_SIZE_BITS + lod;
        int chunkZ = cameraZ >> CHUNK_SIZE_BITS + lod;

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
                (chunkX << chunkSizeBits) - cameraX,
                (chunkY << chunkSizeBits) - cameraY,
                (chunkZ << chunkSizeBits) - cameraZ,
                (chunkX + 1 << chunkSizeBits) - cameraX,
                (chunkY + 1 << chunkSizeBits) - cameraY,
                (chunkZ + 1 << chunkSizeBits) - cameraZ)) return;

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
        computeStartEnd(lod);

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
        int distanceX = Math.abs((cameraX >> CHUNK_SIZE_BITS + lod) - lodModelX);
        int distanceY = Math.abs((cameraY >> CHUNK_SIZE_BITS + lod) - lodModelY);
        int distanceZ = Math.abs((cameraZ >> CHUNK_SIZE_BITS + lod) - lodModelZ);

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

    private void populateOccluders(int lod) {
        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;
                    if (meshCollector.noNeighborHasModel(lodModelX, lodModelY, lodModelZ, lod)) continue;
                    AABB occluder = meshCollector.getOccluder(index, lod);
                    // TODO remove lod == 0
                    if (lod == 0 && occluder != null && occluder.hasVolume()) aabbs.add(occluder);
                }
    }

    private void populateOccludees(int lod) {
        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;
                    AABB occludee = meshCollector.getOccludee(index, lod);
                    if (occludee == null || !occludee.hasVolume()) continue;
                    aabbs.add(occludee);
                    occludee.lod = lod;
                    occludee.index = index;
                }
    }

    private void computeStartEnd(int lod) {
        int lodPlayerX = cameraX >> CHUNK_SIZE_BITS + lod;
        int lodPlayerY = cameraY >> CHUNK_SIZE_BITS + lod;
        int lodPlayerZ = cameraZ >> CHUNK_SIZE_BITS + lod;

        startX = Utils.makeEven(lodPlayerX - RENDER_DISTANCE_XZ - 2);
        startY = Utils.makeEven(lodPlayerY - RENDER_DISTANCE_Y - 2);
        startZ = Utils.makeEven(lodPlayerZ - RENDER_DISTANCE_XZ - 2);

        endX = Utils.makeOdd(lodPlayerX + RENDER_DISTANCE_XZ + 2);
        endY = Utils.makeOdd(lodPlayerY + RENDER_DISTANCE_Y + 2);
        endZ = Utils.makeOdd(lodPlayerZ + RENDER_DISTANCE_XZ + 2);
    }


    private long[] lodVisibilityBits;
    private MeshCollector meshCollector;
    private int cameraX, cameraY, cameraZ;
    private int startX, startY, startZ, endX, endY, endZ;

    private final long[][] visibilityBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
    private final ArrayList<AABB> aabbs = new ArrayList<>(1024);
    private final Rasterizer rasterizer = new Rasterizer();
}
