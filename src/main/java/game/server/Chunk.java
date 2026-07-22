package game.server;

import core.utils.ByteArrayList;
import game.player.interaction.ShapePlaceable;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class Chunk {

    public final long X, Y, Z;
    public final ChunkID ID;
    public final int LOD;
    public int INDEX;

    /**
     * Constructs a new Chunk at the specified chunk coordinates and level of detail.
     *
     * @param chunkX The x-coordinate of the chunk in the world chunk coordinate system. Origin is the world origin. Measured in chunks at the specified level of detail.
     * @param chunkY The y-coordinate of the chunk in the world chunk coordinate system. Origin is the world origin. Measured in chunks at the specified level of detail.
     * @param chunkZ The z-coordinate of the chunk in the world chunk coordinate system. Origin is the world origin. Measured in chunks at the specified level of detail.
     * @param lod    The level of detail for this chunk.
     */
    public Chunk(long chunkX, long chunkY, long chunkZ, int lod) {
        materials = new MaterialsData(CHUNK_SIZE_BITS, AIR);
        X = chunkX & MAX_CHUNKS_MASK >> lod;
        Y = chunkY & MAX_CHUNKS_MASK >> lod;
        Z = chunkZ & MAX_CHUNKS_MASK >> lod;
        INDEX = Utils.getChunkIndex(X, Y, Z, lod);
        ID = new ChunkID(X, Y, Z, lod);
        LOD = lod;
    }

    public byte getSaveMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        return materials.getMaterial(inChunkX, inChunkY, inChunkZ);
    }

    /**
     * Gets the neighboring chunks of this chunk.
     *
     * @return A ChunkNeighbors object containing the six adjacent chunks.
     */
    public ChunkNeighbors getNeighbors() {
        World world = Game.getWorld();
        return new ChunkNeighbors(
                world.getChunk(X, Y, Z + 1, LOD),
                world.getChunk(X, Y + 1, Z, LOD),
                world.getChunk(X + 1, Y, Z, LOD),
                world.getChunk(X, Y, Z - 1, LOD),
                world.getChunk(X, Y - 1, Z, LOD),
                world.getChunk(X - 1, Y, Z, LOD));
    }

    /**
     * Stores a material in a specific area within the chunk.
     *
     * @param inChunkX  The x-coordinate within the chunk where the storage starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param inChunkY  The y-coordinate within the chunk where the storage starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param inChunkZ  The z-coordinate within the chunk where the storage starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param countX    The length of the area along the x-axis. Measured in blocks at the specified level of detail.
     * @param countY    The length of the area along the y-axis. Measured in blocks at the specified level of detail.
     * @param countZ    The length of the area along the z-axis. Measured in blocks at the specified level of detail.
     * @param lod       The level of detail for the stored material.
     * @param placeable The ShapePlaceable to be stored.
     */
    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ,
                              int countX, int countY, int countZ, int lod, ShapePlaceable placeable) {
        materials.storeMaterial(inChunkX, inChunkY, inChunkZ, countX, countY, countZ, lod, placeable);
        modified = true;
    }

    /**
     * Generates maps for mesh faces based on the materials in this chunk and its neighbors.
     *
     * @param toMeshFacesMaps      The maps to be populated with mesh face data.
     * @param uncompressedMaterials An array to store uncompressed material data.
     * @param adjacentChunkLayers   An array of ByteArrayLists containing layers from adjacent chunks.
     * @param neighbors             The neighbors of this chunk.
     */
    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, ByteArrayList[] adjacentChunkLayers, ChunkNeighbors neighbors) {
        neighbors.north().materials.fillSideLayerInto(adjacentChunkLayers[NORTH], SOUTH);
        neighbors.top().materials.fillSideLayerInto(adjacentChunkLayers[TOP], BOTTOM);
        neighbors.west().materials.fillSideLayerInto(adjacentChunkLayers[WEST], EAST);
        neighbors.south().materials.fillSideLayerInto(adjacentChunkLayers[SOUTH], NORTH);
        neighbors.bottom().materials.fillSideLayerInto(adjacentChunkLayers[BOTTOM], TOP);
        neighbors.east().materials.fillSideLayerInto(adjacentChunkLayers[EAST], WEST);
        materials.fillUncompressedMaterialsInto(uncompressedMaterials);

        byte[][] adjacentChunkLayersData = {
                adjacentChunkLayers[NORTH].getData(), adjacentChunkLayers[TOP].getData(), adjacentChunkLayers[WEST].getData(),
                adjacentChunkLayers[SOUTH].getData(), adjacentChunkLayers[BOTTOM].getData(), adjacentChunkLayers[EAST].getData()};
        materials.generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayersData);
    }

    /**
     * Stores structure materials in a specific area within the chunk.
     *
     * @param inChunkX  The x-coordinate within the chunk where the structure starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param inChunkY  The y-coordinate within the chunk where the structure starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param inChunkZ  The z-coordinate within the chunk where the structure starts. Origin is the chunk origin. Measured in blocks at the specified level of detail.
     * @param startX    The x-coordinate within the structure. Measured in blocks at the structure's level of detail.
     * @param startY    The y-coordinate within the structure. Measured in blocks at the structure's level of detail.
     * @param startZ    The z-coordinate within the structure. Measured in blocks at the structure's level of detail.
     * @param lengthX   The length of the structure area along the x-axis. Measured in blocks at the specified level of detail.
     * @param lengthY   The length of the structure area along the y-axis. Measured in blocks at the specified level of detail.
     * @param lengthZ   The length of the structure area along the z-axis. Measured in blocks at the specified level of detail.
     * @param lod       The level of detail for the storage.
     * @param structure The structure to be stored.
     * @param transform The transformation to be applied to the structure.
     */
    public void storeStructureMaterials(int inChunkX, int inChunkY, int inChunkZ,
                                        int startX, int startY, int startZ,
                                        int lengthX, int lengthY, int lengthZ,
                                        int lod, Structure structure, byte transform) {
        this.materials.storeStructureMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                lod, structure, transform);
        modified = true;
    }

    public int getIndex() {
        return INDEX;
    }

    public MaterialsData getMaterials() {
        return materials;
    }

    public void setMaterials(MaterialsData materials) {
        this.materials = materials;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isAir() {
        return materials.isHomogenous(AIR);
    }

    public Status getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(Status status) {
        generationStatus = status;
    }

    private MaterialsData materials;
    private boolean modified;
    private Status generationStatus = Status.NOT_STARTED;
}
