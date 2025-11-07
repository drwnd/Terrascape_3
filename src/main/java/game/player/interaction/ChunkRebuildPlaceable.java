package game.player.interaction;

import core.assets.AssetManager;
import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import game.utils.Status;
import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class ChunkRebuildPlaceable implements Placeable {

    @Override
    public void place(Vector3i position, int lod) {
        affectedChunks.clear();
        if (lod == 0) {
            Chunk toPlaceChunk = new Chunk(position.x >> CHUNK_SIZE_BITS, position.y >> CHUNK_SIZE_BITS, position.z >> CHUNK_SIZE_BITS, 0);
            WorldGeneration.generate(toPlaceChunk);
            this.toPlaceChunk = new Structure(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE, toPlaceChunk.getMaterials());
        }
        int chunkX = position.x >> CHUNK_SIZE_BITS + lod;
        int chunkY = position.y >> CHUNK_SIZE_BITS + lod;
        int chunkZ = position.z >> CHUNK_SIZE_BITS + lod;

        Chunk chunk = new ChunkSaver().load(chunkX, chunkY, chunkZ, lod);
        if (chunk.getGenerationStatus() != Status.DONE) WorldGeneration.generate(chunk);
        Game.getWorld().storeChunk(chunk);

        int inChunkX = position.x >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = position.y >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = position.z >> chunk.LOD & CHUNK_SIZE_MASK;

        chunk.storeMaterials(
                inChunkX, inChunkY, inChunkZ,
                0, 0, 0,
                CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE,
                lod, toPlaceChunk);

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return AssetManager.get(new StructureIdentifier("SampleChunk"));
    }

    @Override
    public boolean intersectsAABB(Vector3i position, Vector3i min, Vector3i max) {
        return false;
    }

    @Override
    public void offsetPosition(Vector3i position) {
        position.x &= ~CHUNK_SIZE_MASK;
        position.y &= ~CHUNK_SIZE_MASK;
        position.z &= ~CHUNK_SIZE_MASK;
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private Structure toPlaceChunk;
}
