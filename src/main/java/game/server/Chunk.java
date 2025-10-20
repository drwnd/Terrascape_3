package game.server;

import game.server.saving.ChunkSaver;
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

//    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
//        if (inChunkX < 0) {
//            Chunk neighbor = Game.getWorld().getChunk(X - 1, Y, Z, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(CHUNK_SIZE + inChunkX, inChunkY, inChunkZ);
//        } else if (inChunkX >= CHUNK_SIZE) {
//            Chunk neighbor = Game.getWorld().getChunk(X + 1, Y, Z, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(inChunkX - CHUNK_SIZE, inChunkY, inChunkZ);
//        }
//        if (inChunkY < 0) {
//            Chunk neighbor = Game.getWorld().getChunk(X, Y - 1, Z, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(inChunkX, CHUNK_SIZE + inChunkY, inChunkZ);
//        } else if (inChunkY >= CHUNK_SIZE) {
//            Chunk neighbor = Game.getWorld().getChunk(X, Y + 1, Z, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(inChunkX, inChunkY - CHUNK_SIZE, inChunkZ);
//        }
//        if (inChunkZ < 0) {
//            Chunk neighbor = Game.getWorld().getChunk(X, Y, Z - 1, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(inChunkX, inChunkY, CHUNK_SIZE + inChunkZ);
//        } else if (inChunkZ >= CHUNK_SIZE) {
//            Chunk neighbor = Game.getWorld().getChunk(X, Y, Z + 1, LOD);
//            if (neighbor == null) return OUT_OF_WORLD;
//            return neighbor.getSaveMaterial(inChunkX, inChunkY, inChunkZ - CHUNK_SIZE);
//        }
//
//        return getSaveMaterial(inChunkX, inChunkY, inChunkZ);
//    }

    public void generateSurroundingChunks() {
        World world = Game.getWorld();
        ChunkSaver saver = new ChunkSaver();

        world.loadAndGenerate(X, Y, Z - 1, LOD, saver);
        world.loadAndGenerate(X, Y, Z + 1, LOD, saver);
        world.loadAndGenerate(X, Y - 1, Z, LOD, saver);
        world.loadAndGenerate(X, Y + 1, Z, LOD, saver);
        world.loadAndGenerate(X - 1, Y, Z, LOD, saver);
        world.loadAndGenerate(X + 1, Y, Z, LOD, saver);
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size) {
        materials.storeMaterial(inChunkX, inChunkY, inChunkZ, material, 1 << size);
        modified = true;
    }

    public void storeStructureMaterials(int inChunkX, int inChunkY, int inChunkZ,
                                        int startX, int startY, int startZ,
                                        int lengthX, int lengthY, int lengthZ,
                                        int lod, MaterialsData source) {
        this.materials.storeStructureMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                lod, source);
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

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated() {
        this.generated = true;
    }

    private MaterialsData materials;

    private boolean generated, modified;
}
