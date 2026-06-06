package game.player.rendering;

import static game.player.rendering.MeshGenerator.INTS_PER_VERTEX;
import static game.utils.Constants.*;

record Vertex(int x, int y, int z, int textureData) {

    public static int compare(Vertex a, Vertex b, int cameraX, int cameraY, int cameraZ, int lod) {
        return a.squareDistanceFrom(cameraX, cameraY, cameraZ, lod) - b.squareDistanceFrom(cameraX, cameraY, cameraZ, lod);
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


    private int squareDistanceFrom(int cameraX, int cameraY, int cameraZ, int lod) {
        int side = textureData >> 8 & 7;
        int faceSize1 = (textureData >> 17 & 63) + 1;
        int faceSize2 = (textureData >> 11 & 63) + 1;

        int distanceX = distanceX(cameraX, side, faceSize1, faceSize2);
        int distanceY = distanceY(cameraY, side, faceSize1, faceSize2);
        int distanceZ = distanceZ(cameraZ, side, faceSize1, faceSize2);
        return distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;
    }

    private int distanceX(int cameraX, int side, int faceSize1, int faceSize2) {
        return Math.abs(switch (side) {
            case NORTH, SOUTH -> cameraX - x - faceSize2;
            case TOP, BOTTOM -> cameraX - x - faceSize1;
            default -> cameraX - x;
        });
    }

    private int distanceY(int cameraY, int side, int faceSize1, int faceSize2) {
        return Math.abs(switch (side) {
            case NORTH, SOUTH -> cameraY - y - faceSize1;
            case WEST, EAST ->  cameraY - y - faceSize2;
            default -> cameraY - y;
        });
    }

    private int distanceZ(int cameraZ, int side, int faceSize1, int faceSize2) {
        return Math.abs(switch (side) {
            case TOP, BOTTOM -> cameraZ - z - faceSize2;
            case WEST, EAST -> cameraZ - z - faceSize1;
            default -> cameraZ - z;
        });
    }
}
