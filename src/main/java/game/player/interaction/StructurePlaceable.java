package game.player.interaction;

import core.assets.AssetManager;
import core.utils.MathUtils;
import core.utils.Vector3l;

import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class StructurePlaceable implements Placeable {

    public StructurePlaceable(StructureIdentifier identifier) {
        this.identifier = identifier;
        this.structure = AssetManager.get(identifier);
    }

    @Override
    public void place(Vector3l position, int lod) {
        affectedChunks.clear();
        long chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(position.x + structure.sizeX() >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(position.y + structure.sizeY() >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + structure.sizeZ() >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (long chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (long chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
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
    public void offsetPosition(Vector3l position) {
        position.x -= structure.sizeX() >> 1;
        position.z -= structure.sizeZ() >> 1;
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        min.sub(position);
        max.sub(position);

        for (long structureX = min.x; structureX < max.x; structureX++)
            for (long structureY = min.y; structureY < max.y; structureY++)
                for (long structureZ = min.z; structureZ < max.z; structureZ++) {
                    byte material = structure.getMaterial(structureX, structureY, structureZ);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
                }
        return false;
    }


    public StructureIdentifier getIdentifier() {
        return identifier;
    }


    private void placeInChunk(Chunk chunk, Vector3l position) {
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        long positionX = position.x;
        long positionY = position.y;
        long positionZ = position.z;

        int inChunkX = (int) Math.max(chunkStartX, positionX) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = (int) Math.max(chunkStartY, positionY) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = (int) Math.max(chunkStartZ, positionZ) >> chunk.LOD & CHUNK_SIZE_MASK;

        int startX = (int) (chunkStartX + (inChunkX << chunk.LOD) - positionX) >> chunk.LOD;
        int startY = (int) (chunkStartY + (inChunkY << chunk.LOD) - positionY) >> chunk.LOD;
        int startZ = (int) (chunkStartZ + (inChunkZ << chunk.LOD) - positionZ) >> chunk.LOD;

        int lengthX = (int) MathUtils.min(structure.sizeX() - startX, (long) CHUNK_SIZE - inChunkX << chunk.LOD, structure.sizeX());
        int lengthY = (int) MathUtils.min(structure.sizeY() - startY, (long) CHUNK_SIZE - inChunkY << chunk.LOD, structure.sizeY());
        int lengthZ = (int) MathUtils.min(structure.sizeZ() - startZ, (long) CHUNK_SIZE - inChunkZ << chunk.LOD, structure.sizeZ());
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
