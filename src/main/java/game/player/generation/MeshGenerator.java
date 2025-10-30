package game.player.generation;

import core.utils.IntArrayList;
import game.player.rendering.Mesh;
import game.server.material.Material;

import static game.utils.Constants.*;
import static game.utils.Constants.CHUNK_SIZE_BITS;

public abstract class MeshGenerator {

    public static boolean isVisible(byte toTestMaterial, byte occludingMaterial) {
        if (toTestMaterial == AIR) return false;
        if (occludingMaterial == AIR) return true;

        if ((Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0) return false;

        if ((Material.getMaterialProperties(toTestMaterial) & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY)
            return toTestMaterial != occludingMaterial;
        return true;
    }


    final void clear() {
        waterVerticesList.clear();
        glassVerticesList.clear();
        for (IntArrayList list : opaqueVerticesLists) list.clear();
    }

    final Mesh loadMesh(int chunkX, int chunkY, int chunkZ, int lod) {
        int[] vertexCounts = new int[opaqueVerticesLists.length];
        int[] opaqueVertices = loadOpaqueVertices(vertexCounts);
        int[] transparentVertices = loadTransparentVertices();

        return new Mesh(opaqueVertices, vertexCounts, transparentVertices, waterVerticesList.size(), glassVerticesList.size(), chunkX, chunkY, chunkZ, lod);
    }

    final void addNorthSouthLayer(int side, int materialZ, byte[] toMesh, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = toMesh[materialX << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, toMesh, materialY + 1, materialX, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, toMesh, materialX + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
            }
    }

    final void addTopBottomLayer(int side, int materialY, byte[] toMesh, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialZ < CHUNK_SIZE;
                 materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = toMesh[materialX << CHUNK_SIZE_BITS | materialZ];
                int faceEndZ = growFace1stDirection(toMeshFacesMap, toMesh, materialZ + 1, materialX, material);
                long mask = getMask(faceEndZ - materialZ + 1, materialZ);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, toMesh, materialX + 1, mask, materialZ, faceEndZ, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addFace(side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
            }
    }

    final void addWestEastLayer(int side, int materialX, byte[] toMesh, long[] toMeshFacesMap) {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ])) {

                byte material = toMesh[materialZ << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, toMesh, materialY + 1, materialZ, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndZ = growFace2ndDirection(toMeshFacesMap, toMesh, materialZ + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialZ, faceEndZ);
                addFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
            }
    }


    private int[] loadTransparentVertices() {
        int[] transparentVertices = new int[waterVerticesList.size() + glassVerticesList.size()];
        waterVerticesList.copyInto(transparentVertices, 0);
        glassVerticesList.copyInto(transparentVertices, waterVerticesList.size());
        return transparentVertices;
    }

    private int[] loadOpaqueVertices(int[] vertexCounts) {
        int totalVertexCount = 0, verticesIndex = 0;
        for (IntArrayList vertexList : opaqueVerticesLists) totalVertexCount += vertexList.size();
        int[] opaqueVertices = new int[totalVertexCount];

        for (int index = 0; index < opaqueVerticesLists.length; index++) {
            IntArrayList vertexList = opaqueVerticesLists[index];
            vertexCounts[index] = vertexList.size() * 3;
            vertexList.copyInto(opaqueVertices, verticesIndex);
            verticesIndex += vertexList.size();
        }
        return opaqueVertices;
    }

    private void addFace(int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        if (Material.isGlass(material))
            addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else if (material == WATER)
            addFace(waterVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else
            addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
    }


    private static int growFace1stDirection(long[] toMeshFacesMap, byte[] toMesh, int growStart, int fixedStart, byte material) {
        for (; growStart < CHUNK_SIZE; growStart++) {
            int index = fixedStart << CHUNK_SIZE_BITS | growStart;
            if ((toMeshFacesMap[fixedStart] & 1L << growStart) == 0 || toMesh[index] != material) return growStart - 1;
        }
        return CHUNK_SIZE - 1;
    }

    private static long getMask(int length, int offset) {
        return length == CHUNK_SIZE ? -1L : (1L << length) - 1 << offset;
    }

    private static int growFace2ndDirection(long[] toMeshFacesMap, byte[] toMesh, int growStart, long mask, int fixedStart, int fixedEnd, byte material) {
        for (; growStart < CHUNK_SIZE && (toMeshFacesMap[growStart] & mask) == mask; growStart++)
            for (int index = fixedStart; index <= fixedEnd; index++)
                if (toMesh[growStart << CHUNK_SIZE_BITS | index] != material) return growStart - 1;
        return growStart - 1;
    }

    private static void removeFromBitMap(long[] toMeshFacesMap, long mask, int start, int end) {
        mask = ~mask;
        for (int index = start; index <= end; index++) toMeshFacesMap[index] &= mask;
    }

    private static void addFace(IntArrayList vertices, int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        vertices.add(faceSize1 << 24 | faceSize2 << 18 | materialX << 12 | materialY << 6 | materialZ);
        vertices.add(side << 8 | material & 0xFF);
    }

    private static final int EXPECTED_LIST_SIZE = CHUNK_SIZE * CHUNK_SIZE;
    final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
    final long[][][] toMeshFacesMap = new long[6][CHUNK_SIZE][CHUNK_SIZE];
    final byte[][] adjacentChunkLayers = new byte[6][CHUNK_SIZE * CHUNK_SIZE];
    final byte[] materialsLayer = new byte[CHUNK_SIZE * CHUNK_SIZE];

    final IntArrayList waterVerticesList = new IntArrayList(EXPECTED_LIST_SIZE), glassVerticesList = new IntArrayList(0);
    final IntArrayList[] opaqueVerticesLists = new IntArrayList[]{
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE),
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE)};

}
