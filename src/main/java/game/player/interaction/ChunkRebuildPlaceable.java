package game.player.interaction;

import core.assets.AssetManager;
import core.utils.Saver;
import core.utils.Vector3l;

import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class ChunkRebuildPlaceable implements Placeable {

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 3);
    }

    public static ChunkRebuildPlaceable load(Saver<?> saver) {
        return new ChunkRebuildPlaceable();
    }

    @Override
    public void place(Vector3l position, int lod) {
        if (lod == 0) {
            Chunk toPlaceChunk = new Chunk(position.x >>> CHUNK_SIZE_BITS, position.y >>> CHUNK_SIZE_BITS, position.z >>> CHUNK_SIZE_BITS, 0);
            WorldGeneration.generate(toPlaceChunk);
            this.toPlaceChunk = new Structure(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE, toPlaceChunk.getMaterials());
        }
        long chunkX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkZ = position.z >>> CHUNK_SIZE_BITS + lod;

        Chunk chunk = new ChunkSaver().loadAndGenerate(chunkX, chunkY, chunkZ, lod);

        int inChunkX = (int) position.x >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = (int) position.y >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = (int) position.z >> chunk.LOD & CHUNK_SIZE_MASK;

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
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        return false;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        position.x &= ~CHUNK_SIZE_MASK;
        position.y &= ~CHUNK_SIZE_MASK;
        position.z &= ~CHUNK_SIZE_MASK;
    }

    @Override
    public void spawnParticles(Vector3l position) {
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private Structure toPlaceChunk;
}
