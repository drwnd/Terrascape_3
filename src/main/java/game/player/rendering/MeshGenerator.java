package game.player.rendering;

import core.utils.ByteArrayList;
import core.utils.IntArrayList;

import game.server.Chunk;
import game.server.ChunkNeighbors;
import game.server.Game;
import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Material;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class MeshGenerator {

    public static final int INTS_PER_VERTEX = 4;
    public static final int VERTICES_PER_QUAD = 3; // for 1 Triangle each 3 Vertices
    public static final int PROPERTIES_OFFSET = 24;
    public static final byte OPAQUE = GRASS;

/**
 * Checks whether visible.
 *
 * @param toTestMaterial parameter
 * @param occludingMaterial parameter
 * @return true if the condition holds
 */
    public static boolean isVisible(byte toTestMaterial, byte occludingMaterial) {
        if (toTestMaterial == AIR) return false;
        if (occludingMaterial == AIR) return true;

        if ((Material.getProperties(occludingMaterial) & TRANSPARENT) == 0) return false;

        if ((Material.getProperties(toTestMaterial) & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY)
            return toTestMaterial != occludingMaterial;
        return true;
    }

/**
 * Generates mesh.
 *
 * @param chunk parameter
 * @return result
 */
    public Mesh generateMesh(Chunk chunk) {
        if (chunk.isAir()) return new Mesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD);

        ChunkNeighbors neighbors = chunk.getNeighbors();
        if (neighbors.areUnGenerated()) {
            Game.getServer().scheduleGeneratorRestart();
            return null;
        }

        AABB occluder = chunk.getMaterials().getOccluder();
        chunk.generateToMeshFacesMaps(toMeshFacesMaps, materials, adjacentChunkLayers, neighbors);

        xStart = (int) chunk.X << CHUNK_SIZE_BITS;
        yStart = (int) chunk.Y << CHUNK_SIZE_BITS;
        zStart = (int) chunk.Z << CHUNK_SIZE_BITS;

        clear();
        addNorthSouthFaces();
        addTopBottomFaces();
        addWestEastFaces();
        AABB occludee = getOccludee();
        if (chunk.LOD != 0 && hasOpaqueMesh()) addSideLayers();

        return loadMesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD, occluder, occludee);
    }

/**
 * Generates mesh.
 *
 * @param structure parameter
 * @return result
 */
    public Mesh generateMesh(Structure structure) {

        int endX = structure.sizeX();
        int endY = structure.sizeY();
        int endZ = structure.sizeZ();
        clear();

        for (xStart = 0; xStart < endX; xStart += CHUNK_SIZE)
            for (yStart = 0; yStart < endY; yStart += CHUNK_SIZE)
                for (zStart = 0; zStart < endZ; zStart += CHUNK_SIZE) {
                    int chunkX = xStart >>> CHUNK_SIZE_BITS;
                    int chunkY = yStart >>> CHUNK_SIZE_BITS;
                    int chunkZ = zStart >>> CHUNK_SIZE_BITS;
                    structure.materials().fillUncompressedMaterialsInto(materials,
                            0, 0, 0,
                            xStart, yStart, zStart,
                            CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);
                    structure.materials().generateToMeshFacesMaps(toMeshFacesMaps, materials, chunkX, chunkY, chunkZ);

                    addNorthSouthFaces();
                    addTopBottomFaces();
                    addWestEastFaces();
                }
        return loadMesh(0, 0, 0, 0, null, null);
    }


/**
 * Performs clear.
 */
    private void clear() {
        transparentVerticesList.clear();
        glassVerticesList.clear();
        for (IntArrayList list : opaqueVerticesLists) list.clear();
        for (ByteArrayList list : adjacentChunkLayers) list.clear();
    }

/**
 * Performs load mesh.
 *
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @param occluder parameter
 * @param occludee parameter
 * @return result
 */
    private Mesh loadMesh(long chunkX, long chunkY, long chunkZ, int lod, AABB occluder, AABB occludee) {
        int[] vertexCounts = new int[opaqueVerticesLists.length];
        int[] opaqueVertices = loadOpaqueVertices(vertexCounts);
        int[] transparentVertices = loadTransparentVertices();

        return new Mesh(opaqueVertices, vertexCounts, transparentVertices,
                transparentVerticesList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX,
                glassVerticesList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX,
                chunkX, chunkY, chunkZ, lod, occluder, occludee);
    }

/**
 * Performs load transparent vertices.
 * @return array result
 */
    private int[] loadTransparentVertices() {
        int[] transparentVertices = new int[transparentVerticesList.size() + glassVerticesList.size()];
        transparentVerticesList.copyInto(transparentVertices, 0);
        glassVerticesList.copyInto(transparentVertices, transparentVerticesList.size());
        return transparentVertices;
    }

/**
 * Performs load opaque vertices.
 *
 * @param vertexCounts parameter
 * @return array result
 */
    private int[] loadOpaqueVertices(int[] vertexCounts) {
        int totalVertexCount = 0, verticesIndex = 0;
        for (IntArrayList vertexList : opaqueVerticesLists) totalVertexCount += vertexList.size();
        int[] opaqueVertices = new int[totalVertexCount];

        for (int index = 0; index < opaqueVerticesLists.length; index++) {
            IntArrayList vertexList = opaqueVerticesLists[index];
            vertexCounts[index] = vertexList.size() * VERTICES_PER_QUAD / INTS_PER_VERTEX;
            vertexList.copyInto(opaqueVertices, verticesIndex);
            verticesIndex += vertexList.size();
        }
        return opaqueVertices;
    }

/**
 * Checks whether the object has opaque mesh.
 * @return true if the condition holds
 */
    private boolean hasOpaqueMesh() {
        for (IntArrayList verticesList : opaqueVerticesLists) if (!verticesList.isEmpty()) return true;
        return false;
    }


/**
 * Returns the occludee.
 * @return result
 */
    private AABB getOccludee() {
        AABB occludee = AABB.newMinChunkAABB();

        for (IntArrayList vertices : opaqueVerticesLists) addToAABB(vertices, occludee);
        addToAABB(transparentVerticesList, occludee);
        addToAABB(glassVerticesList, occludee);

        occludee.maxX += 1;
        occludee.maxY += 1;
        occludee.maxZ += 1;
        occludee.minX -= 1;
        occludee.minY -= 1;
        occludee.minZ -= 1;
        return occludee;
    }

/**
 * Adds to aabb.
 *
 * @param vertices parameter
 * @param aabb parameter
 */
    private static void addToAABB(IntArrayList vertices, AABB aabb) {
        int[] data = vertices.getData();
        for (int index = 0; index < vertices.size(); index += INTS_PER_VERTEX) {
            int x = data[index + 0] & CHUNK_SIZE_MASK;
            int y = data[index + 1] & CHUNK_SIZE_MASK;
            int z = data[index + 2] & CHUNK_SIZE_MASK;
            int faceData = data[index + 3];
            addToAABB(aabb, x, y, z, faceData);
        }
    }

/**
 * Adds to aabb.
 *
 * @param aabb parameter
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 * @param faceData parameter
 */
    private static void addToAABB(AABB aabb, int x, int y, int z, int faceData) {
        int side = faceData >> 8 & 7;
        int faceSize1 = (faceData >> 17 & 63) + 1, faceSize2 = (faceData >> 11 & 63) + 1;

        int maxX = x + switch (side) {
            case NORTH, SOUTH -> faceSize2;
            case TOP, BOTTOM -> faceSize1;
            case WEST -> 1;
            default -> 0;
        };
        int maxY = y + switch (side) {
            case NORTH, WEST, SOUTH, EAST -> faceSize1;
            case TOP -> 1;
            default -> 0;
        };
        int maxZ = z + switch (side) {
            case TOP, BOTTOM, WEST, EAST -> faceSize2;
            case NORTH -> 1;
            default -> 0;
        };

        aabb.min(x, y, z);
        aabb.max(maxX, maxY, maxZ);
    }


/**
 * Adds north south faces.
 */
    private void addNorthSouthFaces() {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(materialZ);
            addNorthSouthLayer(NORTH, materialZ, toMeshFacesMaps[NORTH][materialZ]);
            addNorthSouthLayer(SOUTH, materialZ, toMeshFacesMaps[SOUTH][materialZ]);
        }
    }

/**
 * Copies materials north south.
 *
 * @param materialZ Z coordinate in local block coordinates
 */
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

/**
 * Adds top bottom faces.
 */
    private void addTopBottomFaces() {
        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(materialY);
            addTopBottomLayer(TOP, materialY, toMeshFacesMaps[TOP][materialY]);
            addTopBottomLayer(BOTTOM, materialY, toMeshFacesMaps[BOTTOM][materialY]);
        }
    }

/**
 * Copies materials top bottom.
 *
 * @param materialY Y coordinate in local block coordinates
 */
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

/**
 * Adds west east faces.
 */
    private void addWestEastFaces() {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(materialX);
            addWestEastLayer(WEST, materialX, toMeshFacesMaps[WEST][materialX]);
            addWestEastLayer(EAST, materialX, toMeshFacesMaps[EAST][materialX]);
        }
    }

/**
 * Copies materials west east.
 *
 * @param materialX X coordinate in local block coordinates
 */
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


/**
 * Adds north south layer.
 *
 * @param side parameter
 * @param materialZ Z coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
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

/**
 * Adds top bottom layer.
 *
 * @param side parameter
 * @param materialY Y coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
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

/**
 * Adds west east layer.
 *
 * @param side parameter
 * @param materialX X coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
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


/**
 * Adds side layers.
 */
    private void addSideLayers() {
        long[] toMeshFacesMap = toMeshFacesMaps[0][0];

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsNorthSouthSideLayer(0);
        addNorthSouthSideLayer(SOUTH, 0, toMeshFacesMap);

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsNorthSouthSideLayer(CHUNK_SIZE - 1);
        addNorthSouthSideLayer(NORTH, CHUNK_SIZE - 1, toMeshFacesMap);

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsTopBottomSideLayer(0);
        addTopBottomSideLayer(BOTTOM, 0, toMeshFacesMap);

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsTopBottomSideLayer(CHUNK_SIZE - 1);
        addTopBottomSideLayer(TOP, CHUNK_SIZE - 1, toMeshFacesMap);

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsWestEastSideLayer(0);
        addWestEastSideLayer(EAST, 0, toMeshFacesMap);

        Arrays.fill(toMeshFacesMap, -1L);
        copyMaterialsWestEastSideLayer(CHUNK_SIZE - 1);
        addWestEastSideLayer(WEST, CHUNK_SIZE - 1, toMeshFacesMap);
    }

/**
 * Copies materials north south side layer.
 *
 * @param materialZ Z coordinate in local block coordinates
 */
    private void copyMaterialsNorthSouthSideLayer(int materialZ) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                materialsLayer[materialX << CHUNK_SIZE_BITS | materialY] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
    }

/**
 * Copies materials top bottom side layer.
 *
 * @param materialY Y coordinate in local block coordinates
 */
    private void copyMaterialsTopBottomSideLayer(int materialY) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                materialsLayer[materialX << CHUNK_SIZE_BITS | materialZ] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
    }

/**
 * Copies materials west east side layer.
 *
 * @param materialX X coordinate in local block coordinates
 */
    private void copyMaterialsWestEastSideLayer(int materialX) {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                materialsLayer[materialZ << CHUNK_SIZE_BITS | materialY] = materials[MaterialsData.getUncompressedIndex(materialX, materialY, materialZ)];
    }

/**
 * Adds north south side layer.
 *
 * @param side parameter
 * @param materialZ Z coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
    private void addNorthSouthSideLayer(int side, int materialZ, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = materialsLayer[materialX << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, materialY + 1, materialX, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, materialX + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addSideFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
            }
    }

/**
 * Adds top bottom side layer.
 *
 * @param side parameter
 * @param materialY Y coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
    private void addTopBottomSideLayer(int side, int materialY, long[] toMeshFacesMap) {
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX]);
                 materialZ < CHUNK_SIZE;
                 materialZ = Long.numberOfTrailingZeros(toMeshFacesMap[materialX])) {

                byte material = materialsLayer[materialX << CHUNK_SIZE_BITS | materialZ];
                int faceEndZ = growFace1stDirection(toMeshFacesMap, materialZ + 1, materialX, material);
                long mask = getMask(faceEndZ - materialZ + 1, materialZ);
                int faceEndX = growFace2ndDirection(toMeshFacesMap, materialX + 1, mask, materialZ, faceEndZ, material);

                removeFromBitMap(toMeshFacesMap, mask, materialX, faceEndX);
                addSideFace(side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
            }
    }

/**
 * Adds west east side layer.
 *
 * @param side parameter
 * @param materialX X coordinate in local block coordinates
 * @param toMeshFacesMap parameter
 */
    private void addWestEastSideLayer(int side, int materialX, long[] toMeshFacesMap) {
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ]);
                 materialY < CHUNK_SIZE;
                 materialY = Long.numberOfTrailingZeros(toMeshFacesMap[materialZ])) {

                byte material = materialsLayer[materialZ << CHUNK_SIZE_BITS | materialY];
                int faceEndY = growFace1stDirection(toMeshFacesMap, materialY + 1, materialZ, material);
                long mask = getMask(faceEndY - materialY + 1, materialY);
                int faceEndZ = growFace2ndDirection(toMeshFacesMap, materialZ + 1, mask, materialY, faceEndY, material);

                removeFromBitMap(toMeshFacesMap, mask, materialZ, faceEndZ);
                addSideFace(side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
            }
    }

/**
 * Adds side face.
 *
 * @param side parameter
 * @param materialX X coordinate in local block coordinates
 * @param materialY Y coordinate in local block coordinates
 * @param materialZ Z coordinate in local block coordinates
 * @param material parameter
 * @param faceSize1 parameter
 * @param faceSize2 parameter
 */
    private void addSideFace(int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        if ((Material.getProperties(material) & TRANSPARENT) != 0) return;
        addFace(opaqueVerticesLists[6], side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
    }


/**
 * Adds face.
 *
 * @param side parameter
 * @param materialX X coordinate in local block coordinates
 * @param materialY Y coordinate in local block coordinates
 * @param materialZ Z coordinate in local block coordinates
 * @param material parameter
 * @param faceSize1 parameter
 * @param faceSize2 parameter
 */
    private void addFace(int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        int renderingType = Material.getProperties(material) & RENDERING_TYPE_MASK;
        if (renderingType == GLASS_RENDERING)
            addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else if (renderingType == TRANSPARENT_RENDERING)
            addFace(transparentVerticesList, side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
        else //if (renderingType == OPAQUE_RENDERING)
            addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceSize1, faceSize2);
    }

/**
 * Performs grow face1st direction.
 *
 * @param toMeshFacesMap parameter
 * @param growStart parameter
 * @param fixedStart parameter
 * @param material parameter
 * @return result
 */
    private int growFace1stDirection(long[] toMeshFacesMap, int growStart, int fixedStart, byte material) {
        for (; growStart < CHUNK_SIZE; growStart++) {
            int index = fixedStart << CHUNK_SIZE_BITS | growStart;
            if ((toMeshFacesMap[fixedStart] & 1L << growStart) == 0 || materialsLayer[index] != material) return growStart - 1;
        }
        return CHUNK_SIZE - 1;
    }

/**
 * Performs grow face2nd direction.
 *
 * @param toMeshFacesMap parameter
 * @param growStart parameter
 * @param mask parameter
 * @param fixedStart parameter
 * @param fixedEnd parameter
 * @param material parameter
 * @return result
 */
    private int growFace2ndDirection(long[] toMeshFacesMap, int growStart, long mask, int fixedStart, int fixedEnd, byte material) {
        for (; growStart < CHUNK_SIZE && (toMeshFacesMap[growStart] & mask) == mask; growStart++)
            for (int index = fixedStart; index <= fixedEnd; index++)
                if (materialsLayer[growStart << CHUNK_SIZE_BITS | index] != material) return growStart - 1;
        return growStart - 1;
    }


    private static long getMask(int length, int offset) {
        return length == CHUNK_SIZE ? -1L : (1L << length) - 1 << offset;
    }

/**
 * Removes from bit map.
 *
 * @param toMeshFacesMap parameter
 * @param mask parameter
 * @param start parameter
 * @param end parameter
 */
    private static void removeFromBitMap(long[] toMeshFacesMap, long mask, int start, int end) {
        mask = ~mask;
        for (int index = start; index <= end; index++) toMeshFacesMap[index] &= mask;
    }

/**
 * Adds face.
 *
 * @param vertices parameter
 * @param side parameter
 * @param materialX X coordinate in local block coordinates
 * @param materialY Y coordinate in local block coordinates
 * @param materialZ Z coordinate in local block coordinates
 * @param material parameter
 * @param faceSize1 parameter
 * @param faceSize2 parameter
 */
    private void addFace(IntArrayList vertices, int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        vertices.add(xStart | materialX);
        vertices.add(yStart | materialY);
        vertices.add(zStart | materialZ);
        vertices.add(Material.getProperties(material) << PROPERTIES_OFFSET | faceSize1 << 17 | faceSize2 << 11 | side << 8 | material & 0xFF);
    }

    private int xStart, yStart, zStart;

    private final long[][][] toMeshFacesMaps = new long[6][CHUNK_SIZE][CHUNK_SIZE];
    private final byte[] materialsLayer = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
    private final ByteArrayList[] adjacentChunkLayers = new ByteArrayList[]{
            new ByteArrayList(64), new ByteArrayList(64), new ByteArrayList(64),
            new ByteArrayList(64), new ByteArrayList(64), new ByteArrayList(64)
    };

    private static final int EXPECTED_LIST_SIZE = CHUNK_SIZE * CHUNK_SIZE;
    private final IntArrayList transparentVerticesList = new IntArrayList(EXPECTED_LIST_SIZE), glassVerticesList = new IntArrayList(EXPECTED_LIST_SIZE);
    private final IntArrayList[] opaqueVerticesLists = new IntArrayList[]{
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE),
            new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE), new IntArrayList(EXPECTED_LIST_SIZE),
            new IntArrayList(EXPECTED_LIST_SIZE)};
}
