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

    public static final int WIDTH = 400;
    public static final int HEIGHT = 225;

    public RenderingOptimizer() {

    }

    public void computeVisibility(Player player, Matrix4f projectionViewMatrix) {
        FrustumIntersection frustumIntersection = new FrustumIntersection(Transformation.getFrustumCullingMatrix(player.getCamera()));

        this.projectionViewMatrix = projectionViewMatrix;
        meshCollector = player.getMeshCollector();
        Vector3i position = player.getCamera().getPosition().intPosition();

        cameraX = position.x;
        cameraY = position.y;
        cameraZ = position.z;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod);

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccluders(lod);
        Arrays.fill(depthMap, 0.0F);
        renderOccluders();

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccludees(lod);
        testOccludees();
    }

    public long[][] getVisibilityBits() {
        return visibilityBits;
    }

    public float[] getDepthMap() {
        return depthMap;
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

    private void renderOccluders() {
        Vertex a = new Vertex();
        Vertex b = new Vertex();
        Vertex c = new Vertex();
        Vertex d = new Vertex();

        for (AABB occluder : aabbs) {
            renderNorthSide(a, b, c, d, occluder);
//            renderTopSide(a, b, c, d, occluder);
//            renderWestSide(a, b, c, d, occluder);
//            renderSouthSide(a, b, c, d, occluder);
//            renderBottomSide(a, b, c, d, occluder);
//            renderEastSide(a, b, c, d, occluder);
        }
    }

    private void testOccludees() {

    }


    private void renderNorthSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.maxZ);
        b.set(aabb.minX, aabb.maxY, aabb.maxZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.maxX, aabb.minY, aabb.maxZ);
        renderQuad(a, b, c, d);
    }

    private void renderTopSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.maxY, aabb.minZ);
        b.set(aabb.maxX, aabb.maxY, aabb.minZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.minX, aabb.maxY, aabb.maxZ);
        renderQuad(a, b, c, d);
    }

    private void renderWestSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.maxX, aabb.minY, aabb.minZ);
        b.set(aabb.maxX, aabb.minY, aabb.maxZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.maxX, aabb.maxY, aabb.minZ);
        renderQuad(a, b, c, d);
    }

    private void renderSouthSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.maxX, aabb.minY, aabb.minZ);
        c.set(aabb.maxX, aabb.maxY, aabb.minZ);
        d.set(aabb.minX, aabb.maxY, aabb.minZ);
        renderQuad(a, b, c, d);
    }

    private void renderBottomSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.minX, aabb.minY, aabb.maxZ);
        c.set(aabb.maxX, aabb.minY, aabb.maxZ);
        d.set(aabb.maxX, aabb.minY, aabb.minZ);
        renderQuad(a, b, c, d);
    }

    private void renderEastSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.minX, aabb.maxY, aabb.minZ);
        c.set(aabb.minX, aabb.maxY, aabb.maxZ);
        d.set(aabb.minX, aabb.minY, aabb.maxZ);
        renderQuad(a, b, c, d);
    }

    private void renderQuad(Vertex a, Vertex b, Vertex c, Vertex d) {
        a.transform();
        b.transform();
        c.transform();
        d.transform();
        // Problem for later
        if (a.isBehindCamera() || b.isBehindCamera() || c.isBehindCamera() || d.isBehindCamera()) return;

        int minX = minX(a, b, c, d), minY = minY(a, b, c, d);
        int maxX = maxX(a, b, c, d), maxY = maxY(a, b, c, d);

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                if (!isInside(a, b, c, d, x, y)) {
                    depthMap[toDepthIndex(x, y)] = 0.25F;
                    continue;
                }
                depthMap[toDepthIndex(x, y)] = 0.5F;
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

    private static int toDepthIndex(int x, int y) {
        return y * WIDTH + x;
    }

    private static int minX(Vertex a, Vertex b, Vertex c, Vertex d) {
        return Math.max(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)), 0);
    }

    private static int minY(Vertex a, Vertex b, Vertex c, Vertex d) {
        return Math.max(Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)), 0);
    }

    private static int maxX(Vertex a, Vertex b, Vertex c, Vertex d) {
        return Math.min(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)), WIDTH - 1);
    }

    private static int maxY(Vertex a, Vertex b, Vertex c, Vertex d) {
        return Math.min(Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)), HEIGHT - 1);
    }

    private static boolean isInside(Vertex a, Vertex b, Vertex c, Vertex d, int x, int y) {
        return (b.fy - a.fy) * (x - a.fx) + (a.fx - b.fx) * (y - a.fy) >= 0
                && (c.fy - b.fy) * (x - b.fx) + (b.fx - c.fx) * (y - b.fy) >= 0
                && (d.fy - c.fy) * (x - c.fx) + (c.fx - d.fx) * (y - c.fy) >= 0
                && (a.fy - d.fy) * (x - d.fx) + (d.fx - a.fx) * (y - d.fy) >= 0;
    }

    private long[] lodVisibilityBits;
    private MeshCollector meshCollector;
    private Matrix4f projectionViewMatrix;
    private int cameraX, cameraY, cameraZ;
    private int startX, startY, startZ, endX, endY, endZ;

    private final long[][] visibilityBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
    private final float[] depthMap = new float[WIDTH * HEIGHT];
    private final ArrayList<AABB> aabbs = new ArrayList<>(1024);

    private static final int HALF_WIDTH = WIDTH / 2;
    private static final int HALF_HEIGHT = HEIGHT / 2;

    private class Vertex {

        private int x, y, z;
        private float fx, fy, fz;

        private void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void transform() {
            fx = Utils.getWrappedPosition(x, cameraX, WORLD_SIZE_XZ) - (cameraX & ~CHUNK_SIZE_MASK);
            fy = Utils.getWrappedPosition(y, cameraY, WORLD_SIZE_Y) - (cameraY & ~CHUNK_SIZE_MASK);
            fz = Utils.getWrappedPosition(z, cameraZ, WORLD_SIZE_XZ) - (cameraZ & ~CHUNK_SIZE_MASK);

            float newX = fx * projectionViewMatrix.m00() + fy * projectionViewMatrix.m10() + fz * projectionViewMatrix.m20() + projectionViewMatrix.m30();
            float newY = fx * projectionViewMatrix.m01() + fy * projectionViewMatrix.m11() + fz * projectionViewMatrix.m21() + projectionViewMatrix.m31();
            float newZ = fx * projectionViewMatrix.m02() + fy * projectionViewMatrix.m13() + fz * projectionViewMatrix.m22() + projectionViewMatrix.m32();
            float newW = fx * projectionViewMatrix.m03() + fy * projectionViewMatrix.m13() + fz * projectionViewMatrix.m23() + projectionViewMatrix.m33();

            float inverseW = 1.0F / newW;
            fx = newX * inverseW * WIDTH + HALF_WIDTH;
            fy = newY * inverseW * HEIGHT + HALF_HEIGHT;
            fz = newZ * inverseW;

            x = (int) fx;
            y = (int) fy;
        }

        private boolean isBehindCamera() {
            return fz <= 0.0F;
        }
    }
}
