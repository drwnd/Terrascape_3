package game.player.rendering;

import static game.player.rendering.MeshGenerator.INTS_PER_VERTEX;

record Vertex(int x, int y, int z, int textureData) {

    public int manhattanDistanceFrom(int cameraX, int cameraY, int cameraZ, int lod) {
        return Math.abs((x << lod) - cameraX) + Math.abs((y << lod) - cameraY) + Math.abs((z << lod) - cameraZ);
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
}
