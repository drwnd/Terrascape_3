package game.player.rendering;

import game.utils.Utils;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;
import static game.utils.Constants.CHUNK_SIZE_MASK;
import static game.utils.Constants.WORLD_SIZE_XZ;

public final class Rasterizer {

    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;

    public void set(Matrix4f projectionViewMatrix, int cameraX, int cameraY, int cameraZ) {
        this.projectionViewMatrix = projectionViewMatrix;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
    }

    public void renderOccluders(ArrayList<AABB> occluders) {
        Arrays.fill(depthMap, 0.0F);
        Vertex a = new Vertex();
        Vertex b = new Vertex();
        Vertex c = new Vertex();
        Vertex d = new Vertex();

        for (AABB occluder : occluders) {
            if (cameraZ > occluder.minZ) renderNorthSide(a, b, c, d, occluder);
            if (cameraY > occluder.minY) renderTopSide(a, b, c, d, occluder);
            if (cameraX > occluder.minX) renderWestSide(a, b, c, d, occluder);
            if (cameraZ < occluder.maxZ) renderSouthSide(a, b, c, d, occluder);
            if (cameraY < occluder.maxY) renderBottomSide(a, b, c, d, occluder);
            if (cameraX < occluder.maxX) renderEastSide(a, b, c, d, occluder);
        }
    }

    public void testOccludees(ArrayList<AABB> occludees, long[][] visibilityBits) {
        Vertex a = new Vertex();
        Vertex b = new Vertex();
        Vertex c = new Vertex();
        Vertex d = new Vertex();

        for (AABB occludee : occludees) {
            if (testOccludee(a, b, c, d, occludee)) continue;
            visibilityBits[occludee.lod][occludee.index >> 6] &= ~(1L << occludee.index);
        }
    }

    public float[] getDepthMap() {
        return depthMap;
    }


    private boolean testOccludee(Vertex a, Vertex b, Vertex c, Vertex d, AABB occludee) {
        if (cameraZ > occludee.minZ && testNorthSide(a, b, c, d, occludee)) return true;
        if (cameraY > occludee.minY && testTopSide(a, b, c, d, occludee)) return true;
        if (cameraX > occludee.minX && testWestSide(a, b, c, d, occludee)) return true;
        if (cameraZ < occludee.maxZ && testSouthSide(a, b, c, d, occludee)) return true;
        if (cameraY < occludee.maxY && testBottomSide(a, b, c, d, occludee)) return true;
        return cameraX < occludee.maxX && testEastSide(a, b, c, d, occludee);
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
                if (isOutsideQuad(a, b, c, d, x, y)) continue;
                float depth = interpolateDepth(a, b, c, d, x, y);
                int depthIndex = toDepthIndex(x, y);
                if (depthMap[depthIndex] < depth) depthMap[depthIndex] = depth;
            }
    }


    private boolean testNorthSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.maxZ);
        b.set(aabb.minX, aabb.maxY, aabb.maxZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.maxX, aabb.minY, aabb.maxZ);
        return testQuad(a, b, c, d);
    }

    private boolean testTopSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.maxY, aabb.minZ);
        b.set(aabb.maxX, aabb.maxY, aabb.minZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.minX, aabb.maxY, aabb.maxZ);
        return testQuad(a, b, c, d);
    }

    private boolean testWestSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.maxX, aabb.minY, aabb.minZ);
        b.set(aabb.maxX, aabb.minY, aabb.maxZ);
        c.set(aabb.maxX, aabb.maxY, aabb.maxZ);
        d.set(aabb.maxX, aabb.maxY, aabb.minZ);
        return testQuad(a, b, c, d);
    }

    private boolean testSouthSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.maxX, aabb.minY, aabb.minZ);
        c.set(aabb.maxX, aabb.maxY, aabb.minZ);
        d.set(aabb.minX, aabb.maxY, aabb.minZ);
        return testQuad(a, b, c, d);
    }

    private boolean testBottomSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.minX, aabb.minY, aabb.maxZ);
        c.set(aabb.maxX, aabb.minY, aabb.maxZ);
        d.set(aabb.maxX, aabb.minY, aabb.minZ);
        return testQuad(a, b, c, d);
    }

    private boolean testEastSide(Vertex a, Vertex b, Vertex c, Vertex d, AABB aabb) {
        a.set(aabb.minX, aabb.minY, aabb.minZ);
        b.set(aabb.minX, aabb.maxY, aabb.minZ);
        c.set(aabb.minX, aabb.maxY, aabb.maxZ);
        d.set(aabb.minX, aabb.minY, aabb.maxZ);
        return testQuad(a, b, c, d);
    }

    private boolean testQuad(Vertex a, Vertex b, Vertex c, Vertex d) {
        a.transform();
        b.transform();
        c.transform();
        d.transform();
        // Problem for later
        if (a.isBehindCamera() || b.isBehindCamera() || c.isBehindCamera() || d.isBehindCamera()) return true;

        int minX = minX(a, b, c, d), minY = minY(a, b, c, d);
        int maxX = maxX(a, b, c, d), maxY = maxY(a, b, c, d);

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                if (isOutsideQuad(a, b, c, d, x, y)) continue;
                float depth = interpolateDepth(a, b, c, d, x, y);
                int depthIndex = toDepthIndex(x, y);
                if (depthMap[depthIndex] <= depth) return true;
            }
        return false;
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

    private static boolean isOutsideQuad(Vertex a, Vertex b, Vertex c, Vertex d, int x, int y) {
        return !((b.fy - a.fy) * (x - a.fx) + (a.fx - b.fx) * (y - a.fy) >= 0)
                || !((c.fy - b.fy) * (x - b.fx) + (b.fx - c.fx) * (y - b.fy) >= 0)
                || !((d.fy - c.fy) * (x - c.fx) + (c.fx - d.fx) * (y - c.fy) >= 0)
                || !((a.fy - d.fy) * (x - d.fx) + (d.fx - a.fx) * (y - d.fy) >= 0);
    }

    private static float interpolateDepth(Vertex a, Vertex b, Vertex c, Vertex d, int x, int y) {
        if (squareDistance(a, x, y) > squareDistance(c, x, y))
            return interpolateDepth(b, c, d, x, y);
        return interpolateDepth(a, b, d, x, y);
    }

    private static float interpolateDepth(Vertex a, Vertex b, Vertex c, int x, int y) {
        float divisor = (b.fy - c.fy) * (a.fx - c.fx) + (c.fx - b.fx) * (a.fy - c.fy);

        float az = (1.0F - a.fz) * 100, wa = ((b.fy - c.fy) * (x - c.fx) + (c.fx - b.fx) * (y - c.fy)) / divisor;
        float bz = (1.0F - b.fz) * 100, wb = ((c.fy - a.fy) * (x - c.fx) + (a.fx - c.fx) * (y - c.fy)) / divisor;
        float cz = (1.0F - c.fz) * 100, wc = 1.0F - wa - wb;

        return az * wa + bz * wb + cz * wc;
    }

    private static float squareDistance(Vertex vertex, int x, int y) {
        float dx = vertex.fx - x, dy = vertex.fy - y;
        return dx * dx + dy * dy;
    }

    private final float[] depthMap = new float[WIDTH * HEIGHT];
    private Matrix4f projectionViewMatrix;
    private int cameraX, cameraY, cameraZ;

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
            fx = (newX * inverseW * WIDTH + WIDTH) * 0.5F;
            fy = (newY * inverseW * HEIGHT + HEIGHT) * 0.5F;
            fz = newZ * inverseW;

            x = (int) fx;
            y = (int) fy;
        }

        private boolean isBehindCamera() {
            return fz <= Z_NEAR;
        }
    }
}
