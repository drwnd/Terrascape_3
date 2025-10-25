package game.player.rendering;

import core.utils.IntArrayList;

import game.server.Chunk;
import game.server.Game;
import game.server.material.Material;
import game.server.generation.Structure;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class MeshGenerator {

    public MeshGenerator() {
        Material.copyMaterialProperties(materialProperties);
    }

    public void generateMesh(Chunk chunk) {
        Game.getPlayer().getMeshCollector().setMeshed(true, chunk.INDEX, chunk.LOD);
        chunk.generateSurroundingChunks();
        clear();

        chunk.getMaterials().fillUncompressedMaterialsInto(materials);

        addNorthSouthFaces(chunk);
        addTopBottomFaces(chunk);
        addWestEastFaces(chunk);

        Mesh mesh = loadMesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD);
        Game.getPlayer().getMeshCollector().queueMesh(mesh);
    }

    public ArrayList<Mesh> generateMesh(Structure structure) {
        ArrayList<Mesh> meshes = new ArrayList<>();

        int endX = structure.sizeX();
        int endY = structure.sizeY();
        int endZ = structure.sizeZ();

        for (int structureX = 0; structureX < endX; structureX += CHUNK_SIZE)
            for (int structureY = 0; structureY < endY; structureY += CHUNK_SIZE)
                for (int structureZ = 0; structureZ < endZ; structureZ += CHUNK_SIZE) {
                    clear();
                    structure.materials().fillUncompressedMaterialsInto(materials, CHUNK_SIZE_BITS,
                            0, 0, 0,
                            structureX, structureY, structureZ,
                            CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);

                    addNorthSouthFaces(structure, structureX, structureY, structureZ);
                    addTopBottomFaces(structure, structureX, structureY, structureZ);
                    addWestEastFaces(structure, structureX, structureY, structureZ);
                    meshes.add(loadMesh(structureX >> CHUNK_SIZE_BITS, structureY >> CHUNK_SIZE_BITS, structureZ >> CHUNK_SIZE_BITS, 0));
                }
        return meshes;
    }


    private void clear() {
        waterVerticesList.clear();
        glassVerticesList.clear();
        for (IntArrayList list : opaqueVerticesLists) list.clear();
    }

    private Mesh loadMesh(int chunkX, int chunkY, int chunkZ, int lod) {
        int[] vertexCounts = new int[opaqueVerticesLists.length];
        int[] opaqueVertices = loadOpaqueVertices(vertexCounts);
        int[] transparentVertices = loadTransparentVertices();

        return new Mesh(opaqueVertices, vertexCounts, transparentVertices, waterVerticesList.size(), glassVerticesList.size(), chunkX, chunkY, chunkZ, lod);
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


    private void addNorthSouthFaces(Chunk chunk) {
        // Copy materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2, lower, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(chunk, materialZ);
            fillToMeshFacesMaps();
            addNorthSouthLayer(NORTH, materialZ, lower, toMeshFacesMap1);
            addNorthSouthLayer(SOUTH, materialZ, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsNorthSouth(Chunk chunk, int materialZ) {
        if (materialZ == CHUNK_SIZE - 1) chunk.getNeighbor(NORTH).getMaterials().fillUncompressedSideLayerInto(upper, SOUTH);
        else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2 | materialZ + 1 << CHUNK_SIZE_BITS, upper, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);
        }
    }

    private void addTopBottomFaces(Chunk chunk) {
        // Copy materials
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++)
            lower[index] = materials[index << CHUNK_SIZE_BITS];

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(chunk, materialY);
            fillToMeshFacesMaps();
            addTopBottomLayer(TOP, materialY, lower, toMeshFacesMap1);
            addTopBottomLayer(BOTTOM, materialY, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsTopBottom(Chunk chunk, int materialY) {
        if (materialY == CHUNK_SIZE - 1) chunk.getNeighbor(TOP).getMaterials().fillUncompressedSideLayerInto(upper, BOTTOM);
        else {
            for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++)
                upper[index] = materials[index << CHUNK_SIZE_BITS | materialY + 1];
        }
    }

    private void addWestEastFaces(Chunk chunk) {
        // Copy materials
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            System.arraycopy(materials, materialZ << CHUNK_SIZE_BITS, lower, materialZ << CHUNK_SIZE_BITS, CHUNK_SIZE);

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(chunk, materialX);
            fillToMeshFacesMaps();
            addWestEastLayer(WEST, materialX, lower, toMeshFacesMap1);
            addWestEastLayer(EAST, materialX, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsWestEast(Chunk chunk, int materialX) {
        if (materialX == CHUNK_SIZE - 1) chunk.getNeighbor(WEST).getMaterials().fillUncompressedSideLayerInto(upper, EAST);
        else System.arraycopy(materials, materialX + 1 << CHUNK_SIZE_BITS * 2, upper, 0, CHUNK_SIZE * CHUNK_SIZE);
    }


    private void addNorthSouthFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsNorthSouth(structure, structureX, structureY, structureZ, 0, lower);

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(structure, structureX, structureY, structureZ, materialZ + 1, upper);
            fillToMeshFacesMaps();
            addNorthSouthLayer(NORTH, materialZ, lower, toMeshFacesMap1);
            addNorthSouthLayer(SOUTH, materialZ, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsNorthSouth(Structure structure, int structureX, int structureY, int structureZ, int materialZ, byte[] target) {
        if (materialZ < CHUNK_SIZE) {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS, target, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);
        } else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    target[materialX << CHUNK_SIZE_BITS | materialY] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }

    private void addTopBottomFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsTopBottom(structure, structureX, structureY, structureZ, 0, lower);

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(structure, structureX, structureY, structureZ, materialY + 1, upper);
            fillToMeshFacesMaps();
            addTopBottomLayer(TOP, materialY, lower, toMeshFacesMap1);
            addTopBottomLayer(BOTTOM, materialY, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsTopBottom(Structure structure, int structureX, int structureY, int structureZ, int materialY, byte[] target) {
        if (materialY < CHUNK_SIZE) {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    target[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS | materialY];

        } else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    target[materialX << CHUNK_SIZE_BITS | materialZ] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }

    private void addWestEastFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsWestEast(structure, structureX, structureY, structureZ, 0, lower);

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(structure, structureX, structureY, structureZ, materialX + 1, upper);
            fillToMeshFacesMaps();
            addWestEastLayer(WEST, materialX, lower, toMeshFacesMap1);
            addWestEastLayer(EAST, materialX, upper, toMeshFacesMap2);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsWestEast(Structure structure, int structureX, int structureY, int structureZ, int materialX, byte[] target) {
        if (materialX < CHUNK_SIZE) {
            System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2, target, 0, CHUNK_SIZE * CHUNK_SIZE);
        } else {
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    target[materialZ << CHUNK_SIZE_BITS | materialY] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }


    private void addNorthSouthLayer(int side, int materialZ, byte[] toMesh, long[] toMeshFacesMap) {
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

    private void addTopBottomLayer(int side, int materialY, byte[] toMesh, long[] toMeshFacesMap) {
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

    private void addWestEastLayer(int side, int materialX, byte[] toMesh, long[] toMeshFacesMap) {
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


    private void fillToMeshFacesMaps() {
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++) {
            byte lower = this.lower[index];
            byte upper = this.upper[index];
            if (lower != AIR && (upper == AIR || isVisible(lower, upper))) toMeshFacesMap1[index >> 6] |= 1L << index;
            if (upper != AIR && (lower == AIR || isVisible(upper, lower))) toMeshFacesMap2[index >> 6] |= 1L << index;
        }
    }

    private boolean isVisible(byte toTestMaterial, byte occludingMaterial) {
        if ((materialProperties[occludingMaterial & 0xFF] & TRANSPARENT) == 0) return false;

        if ((materialProperties[toTestMaterial & 0xFF] & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY)
            return toTestMaterial != occludingMaterial;
        return true;
    }

    private void addFace(int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        if (Material.isSemiTransparentMaterial(material))
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


    private final byte[] materialProperties = new byte[AMOUNT_OF_MATERIALS];
    private final long[] toMeshFacesMap1 = new long[CHUNK_SIZE];
    private final long[] toMeshFacesMap2 = new long[CHUNK_SIZE];
    private byte[] upper = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private byte[] lower = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    private final IntArrayList waterVerticesList = new IntArrayList(EXPECTED_LIST_SIZE), glassVerticesList = new IntArrayList(0);
    private final IntArrayList[] opaqueVerticesLists = new IntArrayList[]{
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE),
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE)};
}
