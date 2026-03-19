package game.player.interaction;

import core.utils.Vector3l;

import game.player.Player;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;

import game.server.saving.ChunkSaver;

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
    public void place(Vector3l position, int lod) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();

        int mask = -(1 << breakPlaceSize);
        if (Long.numberOfTrailingZeros(position.x & mask) < lod
                || Long.numberOfTrailingZeros(position.y & mask) < lod
                || Long.numberOfTrailingZeros(position.z & mask) < lod) return;

        long chunkX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkZ = position.z >>> CHUNK_SIZE_BITS + lod;

        int inChunkX = (int) position.x >> lod & CHUNK_SIZE_MASK;
        int inChunkY = (int) position.y >> lod & CHUNK_SIZE_MASK;
        int inChunkZ = (int) position.z >> lod & CHUNK_SIZE_MASK;

        int lodSize = Math.max(0, breakPlaceSize - lod);
        mask = -(1 << lodSize);
        inChunkX &= mask;
        inChunkY &= mask;
        inChunkZ &= mask;

        World world = Game.getWorld();
        Chunk chunk = new ChunkSaver().loadAndGenerate(chunkX, chunkY, chunkZ, lod);

        int length = 1 << lodSize;
        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, material, length, length, length);
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
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;

        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
        breakPlaceSize = 1 << breakPlaceSize;
        int mask = -breakPlaceSize;

        long cubeMinX = position.x & mask;
        long cubeMinY = position.y & mask;
        long cubeMinZ = position.z & mask;

        long cubeMaxX = cubeMinX + breakPlaceSize;
        long cubeMaxY = cubeMinY + breakPlaceSize;
        long cubeMaxZ = cubeMinZ + breakPlaceSize;

        return min.x < cubeMaxX && cubeMinX <= max.x
                && min.y < cubeMaxY && cubeMinY <= max.y
                && min.z < cubeMaxZ && cubeMinZ <= max.z;
    }

    @Override
    public void offsetPosition(Vector3l position) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
        int mask = -(1 << breakPlaceSize);
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        int breakPlaceSize = 1 << player.getInteractionHandler().getPlaceBreakSize();
        player.getParticleCollector().addBreakParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, material);
        player.getParticleCollector().addPlaceParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, material);
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final byte material;
}
