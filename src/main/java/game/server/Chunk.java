package game.server;

import game.server.generation.Structure;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class Chunk {

    public final int X, Y, Z, LOD;
    public final long ID;
    public final int INDEX;

    public Chunk(int chunkX, int chunkY, int chunkZ, int lod) {
        materials = new MaterialsData(CHUNK_SIZE_BITS, AIR);
        X = chunkX;
        Y = chunkY;
        Z = chunkZ;
        INDEX = Utils.getChunkIndex(X, Y, Z);
        ID = Utils.getChunkId(X, Y, Z);
        LOD = lod;
    }

    public Chunk getNeighbor(int side) {
        return switch (side) {
            case NORTH -> Game.getWorld().getChunk(X, Y, Z + 1, LOD);
            case TOP -> Game.getWorld().getChunk(X, Y + 1, Z, LOD);
            case WEST -> Game.getWorld().getChunk(X + 1, Y, Z, LOD);
            case SOUTH -> Game.getWorld().getChunk(X, Y, Z - 1, LOD);
            case BOTTOM -> Game.getWorld().getChunk(X, Y - 1, Z, LOD);
            case EAST -> Game.getWorld().getChunk(X - 1, Y, Z, LOD);
            default -> throw new IllegalArgumentException("Side cannot be " + side);
        };
    }

    public byte getSaveMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        return materials.getMaterial(inChunkX, inChunkY, inChunkZ);
    }

    public boolean areSurroundingChunksGenerated() {
        World world = Game.getWorld();
        return world.getGenerationStatus(X, Y, Z + 1, LOD) == Status.DONE
                && world.getGenerationStatus(X, Y, Z - 1, LOD) == Status.DONE
                && world.getGenerationStatus(X, Y + 1, Z, LOD) == Status.DONE
                && world.getGenerationStatus(X, Y - 1, Z, LOD) == Status.DONE
                && world.getGenerationStatus(X + 1, Y, Z, LOD) == Status.DONE
                && world.getGenerationStatus(X - 1, Y, Z, LOD) == Status.DONE;
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size) {
        materials.storeMaterial(inChunkX, inChunkY, inChunkZ, material, 1 << size);
        modified = true;
    }

    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers) {
        getNeighbor(NORTH).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[NORTH], SOUTH);
        getNeighbor(TOP).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[TOP], BOTTOM);
        getNeighbor(WEST).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[WEST], EAST);
        getNeighbor(SOUTH).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[SOUTH], NORTH);
        getNeighbor(BOTTOM).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[BOTTOM], TOP);
        getNeighbor(EAST).materials.fillUncompressedSideLayerInto(adjacentChunkLayers[EAST], WEST);
        materials.fillUncompressedMaterialsInto(uncompressedMaterials);

        materials.generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers);
    }

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

    public void storeMaterials(int inChunkX, int inChunkY, int inChunkZ,
                               int startX, int startY, int startZ,
                               int lengthX, int lengthY, int lengthZ,
                               int lod, Structure structure) {
        this.materials.storeMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                lod, structure);
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
