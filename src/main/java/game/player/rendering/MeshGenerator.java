package game.player.rendering;

import core.utils.IntArrayList;

import game.server.Chunk;
import game.server.Game;
import game.server.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Material;

import static game.utils.Constants.*;

public final class MeshGenerator {

    public static boolean isVisible(byte toTestMaterial, byte occludingMaterial) {
        if (toTestMaterial == AIR) return false;
        if (occludingMaterial == AIR) return true;

        if ((Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0) return false;

        if ((Material.getMaterialProperties(toTestMaterial) & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY)
            return toTestMaterial != occludingMaterial;
        return true;
    }


    public void generateMesh(Chunk chunk) {
        if (chunk.isAir()) {
            Game.getPlayer().getMeshCollector().setMeshed(true, chunk.INDEX, chunk.LOD);
            Mesh mesh = new Mesh(new int[0], new int[6], new int[0], 0, 0, chunk.X, chunk.Y, chunk.Z, chunk.LOD);
            Game.getPlayer().getMeshCollector().queueMesh(mesh);
            return;
        }
        if (!chunk.areSurroundingChunksGenerated()) {
            Game.getServer().scheduleGeneratorRestart();
            return;
        }
        Game.getPlayer().getMeshCollector().setMeshed(true, chunk.INDEX, chunk.LOD);
        chunk.generateToMeshFacesMaps(toMeshFacesMaps, materials, adjacentChunkLayers);

        xStart = chunk.X << CHUNK_SIZE_BITS;
        yStart = chunk.Y << CHUNK_SIZE_BITS;
        zStart = chunk.Z << CHUNK_SIZE_BITS;

        clear();
        addNorthSouthFaces();
        addTopBottomFaces();
        addWestEastFaces();

        Mesh mesh = loadMesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD);
        Game.getPlayer().getMeshCollector().queueMesh(mesh);
    }

    public Mesh generateMesh(Structure structure) {

        int endX = structure.sizeX();
        int endY = structure.sizeY();
        int endZ = structure.sizeZ();
        clear();

        for (xStart = 0; xStart < endX; xStart += CHUNK_SIZE)
            for (yStart = 0; yStart < endY; yStart += CHUNK_SIZE)
                for (zStart = 0; zStart < endZ; zStart += CHUNK_SIZE) {
                    int chunkX = xStart >> CHUNK_SIZE_BITS;
                    int chunkY = yStart >> CHUNK_SIZE_BITS;
                    int chunkZ = zStart >> CHUNK_SIZE_BITS;
                    structure.materials().fillUncompressedMaterialsInto(materials,
                            0, 0, 0,
                            xStart, yStart, zStart,
                            CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);
                    structure.materials().generateToMeshFacesMaps(toMeshFacesMaps, materials, adjacentChunkLayers, chunkX, chunkY, chunkZ);

                    addNorthSouthFaces();
                    addTopBottomFaces();
                    addWestEastFaces();
                }
        return loadMesh(0, 0, 0, 0);
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

        return new Mesh(opaqueVertices, vertexCounts, transparentVertices,
                waterVerticesList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX,
                glassVerticesList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX,
                chunkX, chunkY, chunkZ, lod);
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

        for (int index = 0; index < 6; index++) {
            IntArrayList vertexList = opaqueVerticesLists[index];
            vertexCounts[index] = vertexList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX;
            vertexList.copyInto(opaqueVertices, verticesIndex);
            verticesIndex += vertexList.size();
        }
        return opaqueVertices;
    }


    private void addNorthSouthFaces() {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(materialZ);
            addNorthSouthLayer(NORTH, materialZ, toMeshFacesMaps[NORTH][materialZ]);
            addNorthSouthLayer(SOUTH, materialZ, toMeshFacesMaps[SOUTH][materialZ]);
        }
    }

    private void copyMaterialsNorthSouth(int materialZ) {
        long[] toMeshFaces1 = toMeshFacesMaps[NORTH][materialZ];
        long[] toMeshFaces2 = toMeshFacesMaps[SOUTH][materialZ];

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            long requiredMaterials = toMeshFaces1[materialX] | toMeshFaces2[materialX];
            for (int materialY = Long.numberOfTrailingZeros(requiredMaterials);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(requiredMaterials)) {
                materialsLayer[materialX << CHUNK_SIZE_BITS | materialY] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
                requiredMaterials &= -2L << materialY;
            }
        }
    }

    private void addTopBottomFaces() {
        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(materialY);
            addTopBottomLayer(TOP, materialY, toMeshFacesMaps[TOP][materialY]);
            addTopBottomLayer(BOTTOM, materialY, toMeshFacesMaps[BOTTOM][materialY]);
        }
    }

    private void copyMaterialsTopBottom(int materialY) {
        long[] toMeshFaces1 = toMeshFacesMaps[TOP][materialY];
        long[] toMeshFaces2 = toMeshFacesMaps[BOTTOM][materialY];

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            long requiredMaterials = toMeshFaces1[materialX] | toMeshFaces2[materialX];
            for (int materialZ = Long.numberOfTrailingZeros(requiredMaterials);
                 materialZ < CHUNK_SIZE;
                 materialZ = Long.numberOfTrailingZeros(requiredMaterials)) {
                materialsLayer[materialX << CHUNK_SIZE_BITS | materialZ] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
                requiredMaterials &= -2L << materialZ;
            }
        }
    }

    private void addWestEastFaces() {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(materialX);
            addWestEastLayer(WEST, materialX, toMeshFacesMaps[WEST][materialX]);
            addWestEastLayer(EAST, materialX, toMeshFacesMaps[EAST][materialX]);
        }
    }

    private void copyMaterialsWestEast(int materialX) {
        long[] toMeshFaces1 = toMeshFacesMaps[WEST][materialX];
        long[] toMeshFaces2 = toMeshFacesMaps[EAST][materialX];

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            long requiredMaterials = toMeshFaces1[materialZ] | toMeshFaces2[materialZ];
            for (int materialY = Long.numberOfTrailingZeros(requiredMaterials);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(requiredMaterials)) {
                materialsLayer[materialZ << CHUNK_SIZE_BITS | materialY] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
                requiredMaterials &= -2L << materialY;
            }
        }
    }


    private void addNorthSouthLayer(int side, int materialZ, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = materialsLayer[materialX << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, materialY + 1, materialX, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, materialX + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
            }
    }

    private void addTopBottomLayer(int side, int materialY, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialZ < CHUNK_SIZE;
                 materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = materialsLayer[materialX << CHUNK_SIZE_BITS | materialZ];
                int faceEndZ = growFace1stDirection(toMeshFacesMap, materialZ + 1, materialX, material);
                long mask = getMask(faceEndZ - materialZ + 1, materialZ);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, materialX + 1, mask, materialZ, faceEndZ, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addFace(side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
            }
    }

    private void addWestEastLayer(int side, int materialX, long[] toMeshFacesMap) {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ])) {

                byte material = materialsLayer[materialZ << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, materialY + 1, materialZ, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndZ = growFace2ndDirection(toMeshFacesMap, materialZ + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialZ, faceEndZ);
                addFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
            }
    }


    private void addFace(int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        if (Material.isGlass(material))
            addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else if (material == WATER)
            addFace(waterVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else
            addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
    }

    private int growFace1stDirection(long[] toMeshFacesMap, int growStart, int fixedStart, byte material) {
        for (; growStart < CHUNK_SIZE; growStart++) {
            int index = fixedStart << CHUNK_SIZE_BITS | growStart;
            if ((toMeshFacesMap[fixedStart] & 1L << growStart) == 0 || materialsLayer[index] != material) return growStart - 1;
        }
        return CHUNK_SIZE - 1;
    }

    private int growFace2ndDirection(long[] toMeshFacesMap, int growStart, long mask, int fixedStart, int fixedEnd, byte material) {
        for (; growStart < CHUNK_SIZE && (toMeshFacesMap[growStart] & mask) == mask; growStart++)
            for (int index = fixedStart; index <= fixedEnd; index++)
                if (materialsLayer[growStart << CHUNK_SIZE_BITS | index] != material) return growStart - 1;
        return growStart - 1;
    }


    private static long getMask(int length, int offset) {
        return length == CHUNK_SIZE ? -1L : (1L << length) - 1 << offset;
    }

    private static void removeFromBitMap(long[] toMeshFacesMap, long mask, int start, int end) {
        mask = ~mask;
        for (int index = start; index <= end; index++) toMeshFacesMap[index] &= mask;
    }

    private void addFace(IntArrayList vertices, int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
//        vertices.add(faceSize1 << 24 | faceSize2 << 18 | materialX << 12 | materialY << 6 | materialZ);
//        vertices.add(side << 8 | material & 0xFF);
        vertices.add(xStart | materialX);
        vertices.add(yStart | materialY);
        vertices.add(zStart | materialZ);
        vertices.add(faceSize1 << 24 | faceSize2 << 18 | side << 8 | material & 0xFF);
    }

    private int xStart, yStart, zStart;

    private final long[][][] toMeshFacesMaps = new long[6][CHUNK_SIZE][CHUNK_SIZE];
    private final byte[][] adjacentChunkLayers = new byte[6][CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materialsLayer = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    public static final int INTS_PER_VERTEX = 4;
    public static final int VERTICES_PER_QUAD = 6; // 2 * 3 for 2 Triangles each 3 Vertices
    private static final int EXPECTED_LIST_SIZE = CHUNK_SIZE * CHUNK_SIZE;
    private final IntArrayList waterVerticesList = new IntArrayList(EXPECTED_LIST_SIZE), glassVerticesList = new IntArrayList(EXPECTED_LIST_SIZE);
    private final IntArrayList[] opaqueVerticesLists = new IntArrayList[]{
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE),
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE)};

}
