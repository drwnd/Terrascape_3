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

    public ChunkNeighbors getNeighbors() {
        World world = Game.getWorld();
        return new ChunkNeighbors(this,
                world.getChunk(X, Y, Z + 1, LOD),
                world.getChunk(X, Y + 1, Z, LOD),
                world.getChunk(X + 1, Y, Z, LOD),
                world.getChunk(X, Y, Z - 1, LOD),
                world.getChunk(X, Y - 1, Z, LOD),
                world.getChunk(X - 1, Y, Z, LOD));
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ,
                              int countX, int countY, int countZ, int lod, ShapePlaceable placeable) {
        materials.storeMaterial(inChunkX, inChunkY, inChunkZ, countX, countY, countZ, lod, placeable);
        modified = true;
    }

    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, ByteArrayList[] adjacentChunkLayers, ChunkNeighbors neighbors) {
        neighbors.north().materials.fillSideLayerInto(adjacentChunkLayers[NORTH], SOUTH, 0);
        neighbors.top().materials.fillSideLayerInto(adjacentChunkLayers[TOP], BOTTOM, 0);
        neighbors.west().materials.fillSideLayerInto(adjacentChunkLayers[WEST], EAST, 0);
        neighbors.south().materials.fillSideLayerInto(adjacentChunkLayers[SOUTH], NORTH, 0);
        neighbors.bottom().materials.fillSideLayerInto(adjacentChunkLayers[BOTTOM], TOP, 0);
        neighbors.east().materials.fillSideLayerInto(adjacentChunkLayers[EAST], WEST, 0);
        materials.fillUncompressedMaterialsInto(uncompressedMaterials);

        byte[][] adjacentChunkLayersData = {
                adjacentChunkLayers[NORTH].getData(), adjacentChunkLayers[TOP].getData(), adjacentChunkLayers[WEST].getData(),
                adjacentChunkLayers[SOUTH].getData(), adjacentChunkLayers[BOTTOM].getData(), adjacentChunkLayers[EAST].getData()};
        materials.generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayersData);
    }

    public void storeStructureMaterials(int inChunkX, int inChunkY, int inChunkZ,
                                        int startX, int startY, int startZ,
                                        int lengthX, int lengthY, int lengthZ,
                                        int lod, Structure structure, byte transform, boolean forceOverride) {
        this.materials.storeStructureMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                lod, structure, transform, forceOverride);
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

    public void setModified() {
        modified = true;
    }

    private MaterialsData materials;
    private boolean modified;
    private Status generationStatus = Status.NOT_STARTED;
}
