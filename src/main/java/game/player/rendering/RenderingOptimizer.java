package game.player.rendering;

import game.player.Player;
import game.server.Game;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public RenderingOptimizer() {

    }

    public void computeVisibility(Player player) {
        Matrix4f projectionViewMatrix = Transformation.getFrustumCullingMatrix(player.getCamera());
        FrustumIntersection frustumIntersection = new FrustumIntersection(projectionViewMatrix);

        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        playerX = position.x;
        playerY = position.y;
        playerZ = position.z;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod, playerX >> CHUNK_SIZE_BITS, playerY >> CHUNK_SIZE_BITS, playerZ >> CHUNK_SIZE_BITS);
    }

    public long[][] getVisibilityBits() {
        return visibilityBits;
    }


    private void computeLodVisibility(int lod, FrustumIntersection frustumIntersection, long[][] visibilityBits) {
        int chunkX = playerX >> CHUNK_SIZE_BITS + lod;
        int chunkY = playerY >> CHUNK_SIZE_BITS + lod;
        int chunkZ = playerZ >> CHUNK_SIZE_BITS + lod;

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

    private void removeLodVisibilityOverlap(int lod, int playerChunkX, int playerChunkY, int playerChunkZ) {
        lodVisibilityBits = visibilityBits[lod];
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
        int distanceX = Math.abs((playerX >> CHUNK_SIZE_BITS + lod) - lodModelX);
        int distanceY = Math.abs((playerY >> CHUNK_SIZE_BITS + lod) - lodModelY);
        int distanceZ = Math.abs((playerZ >> CHUNK_SIZE_BITS + lod) - lodModelZ);

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

    private long[] lodVisibilityBits;
    private MeshCollector meshCollector;
    private int playerX, playerY, playerZ;

    private final long[][] visibilityBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
}
