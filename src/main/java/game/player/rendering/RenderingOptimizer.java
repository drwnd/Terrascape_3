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

    private RenderingOptimizer() {

    }

    public static synchronized void computeVisibility(long[][] visibilityBits, Player player) {
        Matrix4f projectionViewMatrix = Transformation.getFrustumCullingMatrix(player.getCamera());
        FrustumIntersection frustumIntersection = new FrustumIntersection(projectionViewMatrix);

        meshCollector = player.getMeshCollector();
        position = player.getPosition().intPosition();
        RenderingOptimizer.visibilityBits = visibilityBits;

        int playerChunkX = position.x >> CHUNK_SIZE_BITS;
        int playerChunkY = position.y >> CHUNK_SIZE_BITS;
        int playerChunkZ = position.z >> CHUNK_SIZE_BITS;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod, playerChunkX, playerChunkY, playerChunkZ);
    }


    private static void computeLodVisibility(int lod, FrustumIntersection frustumIntersection, long[][] visibilityBits) {
        int chunkX = position.x >> CHUNK_SIZE_BITS + lod;
        int chunkY = position.y >> CHUNK_SIZE_BITS + lod;
        int chunkZ = position.z >> CHUNK_SIZE_BITS + lod;

        lodVisibilityBits = visibilityBits[lod];
        Arrays.fill(lodVisibilityBits, 0L);

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
        lodVisibilityBits[chunkIndex >> 6] |= 1L << chunkIndex;

        fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (1 << NORTH), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (1 << SOUTH), lod, frustumIntersection);

        fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (1 << TOP), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (1 << BOTTOM), lod, frustumIntersection);

        fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (1 << WEST), lod, frustumIntersection);
        fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (1 << EAST), lod, frustumIntersection);
    }

    private static void fillVisibleChunks(int chunkX, int chunkY, int chunkZ, byte traveledDirections, int lod, FrustumIntersection intersection) {
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
        if ((lodVisibilityBits[chunkIndex >> 6] & 1L << chunkIndex) != 0) return;

        if (Game.getWorld().getChunk(chunkIndex, lod) == null) return;
        int intersectionType = intersection.intersectAab(
                chunkX << chunkSizeBits, chunkY << chunkSizeBits, chunkZ << chunkSizeBits,
                chunkX + 1 << chunkSizeBits, chunkY + 1 << chunkSizeBits, chunkZ + 1 << chunkSizeBits);
        if (intersectionType != FrustumIntersection.INSIDE && intersectionType != FrustumIntersection.INTERSECT) return;

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

    private static void removeLodVisibilityOverlap(int lod, int playerChunkX, int playerChunkY, int playerChunkZ) {
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

    private static void removeModelVisibilityOverlap(int lod, int lodModelX, int lodModelY, int lodModelZ) {
        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ);
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

    private static boolean modelFarEnoughAway(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        int distanceX = Math.abs((Utils.floor(position.x) >> CHUNK_SIZE_BITS + lod) - lodModelX);
        int distanceY = Math.abs((Utils.floor(position.y) >> CHUNK_SIZE_BITS + lod) - lodModelY);
        int distanceZ = Math.abs((Utils.floor(position.z) >> CHUNK_SIZE_BITS + lod) - lodModelZ);

        return distanceX > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceZ > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceY > (RENDER_DISTANCE_Y >> 1) + 1;
    }

    private static boolean modelCubePresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        return meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ + 1, lod);
    }

    private static void clearModelCubeVisibility(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        long[] visibilityBits = RenderingOptimizer.visibilityBits[lod];
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY + 1, lodModelZ);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY + 1, lodModelZ + 1);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ + 1);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY + 1, lodModelZ);
        visibilityBits[index >> 6] &= ~(1L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY + 1, lodModelZ + 1);
        visibilityBits[index >> 6] &= ~(1L << index);
    }

    private static long[] lodVisibilityBits;
    private static long[][] visibilityBits;
    private static MeshCollector meshCollector;
    private static Vector3i position;
}
