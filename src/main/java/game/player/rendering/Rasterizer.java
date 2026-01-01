package game.player.rendering;

import game.utils.Utils;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;
import static game.utils.Constants.CHUNK_SIZE_MASK;
import static game.utils.Constants.WORLD_SIZE_XZ;

public final class Rasterizer {

    public static final int WIDTH = 144;
    public static final int HEIGHT = 81;

    public Rasterizer() {
        v0 = new Vertex();
        v1 = new Vertex();
        v2 = new Vertex();
        v3 = new Vertex();
        v4 = new Vertex();
        v5 = new Vertex();
        v6 = new Vertex();
        v7 = new Vertex();
    }


    public void set(Matrix4f projectionViewMatrix, int cameraX, int cameraY, int cameraZ) {
        this.projectionViewMatrix = projectionViewMatrix;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
    }

    public void renderOccluders(ArrayList<AABB> occluders) {
        Arrays.fill(depthMap, 0.0F);

        for (AABB occluder : occluders) {
            setVertices(occluder);

            if (cameraZ < occluder.maxZ) renderNorthSide();
            if (cameraY < occluder.maxY) renderTopSide();
            if (cameraX < occluder.maxX) renderWestSide();
            if (cameraZ > occluder.minZ) renderSouthSide();
            if (cameraY > occluder.minY) renderBottomSide();
            if (cameraX > occluder.minX) renderEastSide();
        }
    }

    public void testOccludees(ArrayList<AABB> occludees, long[][] visibilityBits) {
        for (AABB occludee : occludees) {
            if (testOccludee(occludee)) continue;
            visibilityBits[occludee.lod][occludee.index >> 6] &= ~(1L << occludee.index);
        }
    }

    public float[] getDepthMap() {
        return depthMap;
    }


    private void renderNorthSide() {
        a = v1;
        b = v3;
        c = v7;
        d = v5;
        renderQuad();
    }

    private void renderTopSide() {
        a = v2;
        b = v6;
        c = v7;
        d = v3;
        renderQuad();
    }

    private void renderWestSide() {
        a = v4;
        b = v5;
        c = v7;
        d = v6;
        renderQuad();
    }

    private void renderSouthSide() {
        a = v0;
        b = v4;
        c = v6;
        d = v2;
        renderQuad();
    }

    private void renderBottomSide() {
        a = v0;
        b = v1;
        c = v5;
        d = v4;
        renderQuad();
    }

    private void renderEastSide() {
        a = v0;
        b = v2;
        c = v3;
        d = v1;
        renderQuad();
    }

    private void renderQuad() {
        // Problem for later
        if (a.isBehindCamera() || b.isBehindCamera() || c.isBehindCamera() || d.isBehindCamera()) return;

        int minX = minX(), minY = minY();
        int maxX = maxX(), maxY = maxY();

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                if (isOutsideQuad(x, y)) continue;
                float depth = interpolateDepth(x, y);
                int depthIndex = toDepthIndex(x, y);
                if (depthMap[depthIndex] < depth) depthMap[depthIndex] = depth;
            }
    }


    private boolean testOccludee(AABB occludee) {
        setVertices(occludee);

        if (cameraZ < occludee.maxZ && testNorthSide()) return true;
        if (cameraY < occludee.maxY && testTopSide()) return true;
        if (cameraX < occludee.maxX && testWestSide()) return true;
        if (cameraZ > occludee.minZ && testSouthSide()) return true;
        if (cameraY > occludee.minY && testBottomSide()) return true;
        return cameraX > occludee.minX && testEastSide();
    }

    private boolean testNorthSide() {
        a = v1;
        b = v3;
        c = v7;
        d = v5;
        return testQuad();
    }

    private boolean testTopSide() {
        a = v2;
        b = v6;
        c = v7;
        d = v3;
        return testQuad();
    }

    private boolean testWestSide() {
        a = v4;
        b = v5;
        c = v7;
        d = v6;
        return testQuad();
    }

    private boolean testSouthSide() {
        a = v0;
        b = v4;
        c = v6;
        d = v2;
        return testQuad();
    }

    private boolean testBottomSide() {
        a = v0;
        b = v1;
        c = v5;
        d = v4;
        return testQuad();
    }

    private boolean testEastSide() {
        a = v0;
        b = v2;
        c = v3;
        d = v1;
        return testQuad();
    }

    private boolean testQuad() {
        // Problem for later
        if (a.isBehindCamera() || b.isBehindCamera() || c.isBehindCamera() || d.isBehindCamera()) return true;

        int minX = minX(), minY = minY();
        int maxX = maxX(), maxY = maxY();

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                if (isOutsideQuad(x, y)) continue;
                float depth = interpolateDepth(x, y);
                int depthIndex = toDepthIndex(x, y);
                if (depthMap[depthIndex] <= depth) return true;
            }
        return false;
    }


    private int minX() {
        return Math.max(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)), 0);
    }

    private int minY() {
        return Math.max(Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)), 0);
    }

    private int maxX() {
        return Math.min(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)), WIDTH - 1);
    }

    private int maxY() {
        return Math.min(Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)), HEIGHT - 1);
    }

    private boolean isOutsideQuad(int x, int y) {
        return (b.fy - a.fy) * (x - a.fx) + (a.fx - b.fx) * (y - a.fy) > 0
                || (c.fy - b.fy) * (x - b.fx) + (b.fx - c.fx) * (y - b.fy) > 0
                || (d.fy - c.fy) * (x - c.fx) + (c.fx - d.fx) * (y - c.fy) > 0
                || (a.fy - d.fy) * (x - d.fx) + (d.fx - a.fx) * (y - d.fy) > 0;
    }

    private float interpolateDepth(int x, int y) {
        if (squareDistance(a, x, y) > squareDistance(c, x, y))
            return interpolateDepth(b, c, d, x, y);
        return interpolateDepth(a, b, d, x, y);
    }

    private void setVertices(AABB aabb) {
        v0.set(aabb.minX, aabb.minY, aabb.minZ);
        v1.set(aabb.minX, aabb.minY, aabb.maxZ);
        v2.set(aabb.minX, aabb.maxY, aabb.minZ);
        v3.set(aabb.minX, aabb.maxY, aabb.maxZ);
        v4.set(aabb.maxX, aabb.minY, aabb.minZ);
        v5.set(aabb.maxX, aabb.minY, aabb.maxZ);
        v6.set(aabb.maxX, aabb.maxY, aabb.minZ);
        v7.set(aabb.maxX, aabb.maxY, aabb.maxZ);
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

    private static int toDepthIndex(int x, int y) {
        return y * WIDTH + x;
    }


    private final Vertex v0, v1, v2, v3, v4, v5, v6, v7;
    private final float[] depthMap = new float[WIDTH * HEIGHT];

    private Vertex a, b, c, d;
    private Matrix4f projectionViewMatrix;
    private int cameraX, cameraY, cameraZ;

    private class Vertex {

        private int x, y, z;
        private float fx, fy, fz;

        private void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;

            transform();
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
