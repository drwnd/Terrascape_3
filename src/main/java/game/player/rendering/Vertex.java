package game.player.rendering;

import static game.player.rendering.MeshGenerator.INTS_PER_VERTEX;
import static game.utils.Constants.*;

record Vertex(int x, int y, int z, int textureData) {

    public static int compare(Vertex a, Vertex b, long cameraX, long cameraY, long cameraZ, int lod) {
//        if (a.getDirection() == b.getDirection())
//            return Long.signum(a.distanceAlongNormal(cameraX, cameraY, cameraZ, lod) - b.distanceAlongNormal(cameraX, cameraY, cameraZ, lod));
        return Long.signum(a.squareDistanceFrom(cameraX, cameraY, cameraZ, lod) - b.squareDistanceFrom(cameraX, cameraY, cameraZ, lod));
    }

    public static Vertex[] toVertexObjects(int[] verticesData) {
        if (verticesData == null) return null;
        Vertex[] vertices = new Vertex[verticesData.length / INTS_PER_VERTEX];
        for (int index = 0; index < verticesData.length; index += INTS_PER_VERTEX)
            vertices[index >> 2] = new Vertex(verticesData[index], verticesData[index + 1], verticesData[index + 2], verticesData[index + 3]);
        return vertices;
    }

    public static int[] toVertexData(Vertex[] vertices) {
        int[] verticesData = new int[vertices.length * INTS_PER_VERTEX];
        for (int index = 0; index < verticesData.length; index += INTS_PER_VERTEX) {
            Vertex vertex = vertices[index >> 2];
            verticesData[index] = vertex.x();
            verticesData[index + 1] = vertex.y();
            verticesData[index + 2] = vertex.z();
            verticesData[index + 3] = vertex.textureData();
        }
        return verticesData;
    }


    private long distanceAlongNormal(long cameraX, long cameraY, long cameraZ, int lod) {
        int side = textureData >> 8 & 7;
        int addend = side <= WEST ? 1 : 0;

        return Math.abs(switch (side) {
            case NORTH, SOUTH -> cameraZ - ((long) z + addend << lod);
            case TOP, BOTTOM ->  cameraY - ((long) y + addend << lod);
            case WEST, EAST -> cameraX - ((long) x + addend << lod);
            default -> 0;
        });
    }

    private long squareDistanceFrom(long cameraX, long cameraY, long cameraZ, int lod) {
        int side = textureData >> 8 & 7;
        int faceSize1 = Math.max(0, ((textureData >> 17 & 63) + 1 << lod) - 2);
        int faceSize2 = Math.max(0, ((textureData >> 11 & 63) + 1 << lod) - 2);

        long distanceX = distanceX(cameraX, side, faceSize1, faceSize2, lod);
        long distanceY = distanceY(cameraY, side, faceSize1, faceSize2, lod);
        long distanceZ = distanceZ(cameraZ, side, faceSize1, faceSize2, lod);
        return distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;
    }

    private long distanceX(long cameraX, int side, int faceSize1, int faceSize2, int lod) {
        long totalX = ((long) x << lod) + 1;
        if (cameraX <= totalX) return totalX - cameraX;
        return switch (side) {
            case NORTH, SOUTH -> cameraX <= totalX + faceSize2 ? 0 : cameraX - totalX - faceSize2;
            case TOP, BOTTOM -> cameraX <= totalX + faceSize1 ? 0 : cameraX - totalX - faceSize1;
            default -> cameraX - totalX;
        };
    }

    private long distanceY(long cameraY, int side, int faceSize1, int faceSize2, int lod) {
        long totalY = ((long) y << lod) + 1;
        if (cameraY <= totalY) return totalY - cameraY;
        return switch (side) {
            case NORTH, SOUTH -> cameraY <= totalY + faceSize1 ? 0 : cameraY - totalY - faceSize1;
            case WEST, EAST -> cameraY <= totalY + faceSize2 ? 0 : cameraY - totalY - faceSize2;
            default -> cameraY - totalY;
        };
    }

    private long distanceZ(long cameraZ, int side, int faceSize1, int faceSize2, int lod) {
        long totalZ = ((long) z << lod) + 1;
        if (cameraZ <= totalZ) return totalZ - cameraZ;
        return switch (side) {
            case TOP, BOTTOM -> cameraZ <= totalZ + faceSize2 ? 0 : cameraZ - totalZ - faceSize2;
            case WEST, EAST -> cameraZ <= totalZ + faceSize1 ? 0 : cameraZ - totalZ - faceSize1;
            default -> cameraZ - totalZ;
        };
    }

    private int getDirection() {
        int side = textureData >> 8 & 7;
        if (side >= SOUTH) side -= 3;
        return side;
    }
}
