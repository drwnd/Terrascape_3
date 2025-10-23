package game.player.interaction;

import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.utils.Utils;
import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class StructurePlaceable implements Placeable {

    public StructurePlaceable(Structure structure) {
        this.structure = structure;
    }

    @Override
    public void place(Vector3i position, int lod) {
        affectedChunks.clear();
        int chunkStartX = position.x >> lod + CHUNK_SIZE_BITS;
        int chunkStartY = position.y >> lod + CHUNK_SIZE_BITS;
        int chunkStartZ = position.z >> lod + CHUNK_SIZE_BITS;
        int chunkEndX = position.x + structure.sizeX() >> lod + CHUNK_SIZE_BITS;
        int chunkEndY = position.y + structure.sizeY() >> lod + CHUNK_SIZE_BITS;
        int chunkEndZ = position.z + structure.sizeZ() >> lod + CHUNK_SIZE_BITS;

        for (int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (int chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++) {
                    Chunk chunk = Game.getWorld().getChunk(chunkX, chunkY, chunkZ, lod);
                    if (chunk != null) placeInChunk(chunk, position);
                }
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


    private void placeInChunk(Chunk chunk, Vector3i position) {
        int chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkX = Math.max(chunkStartX, position.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = Math.max(chunkStartY, position.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = Math.max(chunkStartZ, position.z) >> chunk.LOD & CHUNK_SIZE_MASK;

        int startX = chunkStartX + (inChunkX << chunk.LOD) - position.x;
        int startY = chunkStartY + (inChunkY << chunk.LOD) - position.y;
        int startZ = chunkStartZ + (inChunkZ << chunk.LOD) - position.z;

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
    }

    private final Structure structure;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
}
