package game.player.interaction;

import core.assets.AssetManager;
import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class StructurePlaceable implements Placeable {

    public StructurePlaceable(StructureIdentifier identifier) {
        this.identifier = identifier;
        this.structure = AssetManager.get(identifier);
    }

    @Override
    public void place(Vector3i position, int lod) {
        affectedChunks.clear();
        int chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        int chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        int chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        int chunkEndX = Utils.getWrappedChunkCoordinate(position.x + structure.sizeX() >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        int chunkEndY = Utils.getWrappedChunkCoordinate(position.y + structure.sizeY() >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        int chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + structure.sizeZ() >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (int chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod), position);
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return structure;
    }

    @Override
    public void offsetPosition(Vector3i position) {
        position.x -= structure.sizeX() >> 1;
        position.z -= structure.sizeZ() >> 1;
    }

    @Override
    public boolean intersectsAABB(Vector3i position, Vector3i min, Vector3i max) {
        min.sub(position);
        max.sub(position);

        for (int structureX = min.x; structureX < max.x; structureX++)
            for (int structureY = min.y; structureY < max.y; structureY++)
                for (int structureZ = min.z; structureZ < max.z; structureZ++) {
                    byte material = structure.getMaterial(structureX, structureY, structureZ);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
                }
        return false;
    }


    public StructureIdentifier getIdentifier() {
        return identifier;
    }


    private void placeInChunk(Chunk chunk, Vector3i position) {
        int chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int positionX = position.x;
        int positionY = position.y;
        int positionZ = position.z;

        int inChunkX = Math.max(chunkStartX, positionX) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = Math.max(chunkStartY, positionY) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = Math.max(chunkStartZ, positionZ) >> chunk.LOD & CHUNK_SIZE_MASK;

        int startX = chunkStartX + (inChunkX << chunk.LOD) - positionX >> chunk.LOD;
        int startY = chunkStartY + (inChunkY << chunk.LOD) - positionY >> chunk.LOD;
        int startZ = chunkStartZ + (inChunkZ << chunk.LOD) - positionZ >> chunk.LOD;

        int lengthX = Utils.min(structure.sizeX() - startX, CHUNK_SIZE - inChunkX << chunk.LOD, structure.sizeX());
        int lengthY = Utils.min(structure.sizeY() - startY, CHUNK_SIZE - inChunkY << chunk.LOD, structure.sizeY());
        int lengthZ = Utils.min(structure.sizeZ() - startZ, CHUNK_SIZE - inChunkZ << chunk.LOD, structure.sizeZ());
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return;

        chunk.storeStructureMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                chunk.LOD, structure, (byte) 0);

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        if (inChunkX == 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        if (inChunkZ == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        if (inChunkX + lengthX == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY + lengthY == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        if (inChunkZ + lengthZ == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    private final StructureIdentifier identifier;
    private final Structure structure;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
}
