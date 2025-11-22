package game.player.interaction;

import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;

import game.server.saving.ChunkSaver;
import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class CubePlaceable implements Placeable {

    public CubePlaceable(byte material) {
        this.material = material;
    }

    public byte getMaterial() {
        return material;
    }

    @Override
    public void place(Vector3i position, int lod) {
        affectedChunks.clear();
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();

        int mask = -(1 << breakPlaceSize);
        if (Integer.numberOfTrailingZeros(position.x & mask) < lod
                || Integer.numberOfTrailingZeros(position.y & mask) < lod
                || Integer.numberOfTrailingZeros(position.z & mask) < lod) return;

        int chunkX = position.x >> CHUNK_SIZE_BITS + lod;
        int chunkY = position.y >> CHUNK_SIZE_BITS + lod;
        int chunkZ = position.z >> CHUNK_SIZE_BITS + lod;

        int inChunkX = position.x >> lod & CHUNK_SIZE_MASK;
        int inChunkY = position.y >> lod & CHUNK_SIZE_MASK;
        int inChunkZ = position.z >> lod & CHUNK_SIZE_MASK;

        int lodSize = Math.max(0, breakPlaceSize - lod);
        mask = -(1 << lodSize);
        inChunkX &= mask;
        inChunkY &= mask;
        inChunkZ &= mask;

        World world = Game.getWorld();
        Chunk chunk = new ChunkSaver().loadAndGenerate(chunkX, chunkY, chunkZ, lod);

        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, material, lodSize);
        affectedChunks.add(chunk);
        if (inChunkX == 0) affectedChunks.add(world.getChunk(chunkX - 1, chunkY, chunkZ, lod));
        if (inChunkY == 0) affectedChunks.add(world.getChunk(chunkX, chunkY - 1, chunkZ, lod));
        if (inChunkZ == 0) affectedChunks.add(world.getChunk(chunkX, chunkY, chunkZ - 1, lod));
        if (inChunkX + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX + 1, chunkY, chunkZ, lod));
        if (inChunkY + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX, chunkY + 1, chunkZ, lod));
        if (inChunkZ + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX, chunkY, chunkZ + 1, lod));
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return new Structure(material);
    }

    @Override
    public boolean intersectsAABB(Vector3i position, Vector3i min, Vector3i max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;

        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
        breakPlaceSize = 1 << breakPlaceSize;
        int mask = -breakPlaceSize;

        int cubeMinX = position.x & mask;
        int cubeMinY = position.y & mask;
        int cubeMinZ = position.z & mask;

        int cubeMaxX = cubeMinX + breakPlaceSize;
        int cubeMaxY = cubeMinY + breakPlaceSize;
        int cubeMaxZ = cubeMinZ + breakPlaceSize;

        return min.x < cubeMaxX && cubeMinX <= max.x
                && min.y < cubeMaxY && cubeMinY <= max.y
                && min.z < cubeMaxZ && cubeMinZ <= max.z;
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final byte material;
}
