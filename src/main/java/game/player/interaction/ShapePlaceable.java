package game.player.interaction;

import core.renderables.Slider;
import core.utils.Vector3l;

import game.player.Player;
import game.server.Chunk;
import game.server.Game;
import game.server.MaterialsData;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;

import java.util.ArrayList;
import java.util.List;

import static game.utils.Constants.*;

public abstract class ShapePlaceable implements Placeable {

    protected ShapePlaceable(byte material) {
        this.material = material;
    }

    public abstract List<Slider<?>> settings();

    public abstract ShapePlaceable copyWithMaterial(byte material);

    public byte getMaterial() {
        return material;
    }

    @Override
    public void place(Vector3l position, int lod) {
        long[] bitMap = getBitMap();
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();

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
        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, material, 1, 1, 1, length, bitMap, lod);
        affectedChunks.add(chunk);
        if (inChunkX == 0) affectedChunks.add(world.getChunk(chunkX - 1, chunkY, chunkZ, lod));
        if (inChunkY == 0) affectedChunks.add(world.getChunk(chunkX, chunkY - 1, chunkZ, lod));
        if (inChunkZ == 0) affectedChunks.add(world.getChunk(chunkX, chunkY, chunkZ - 1, lod));
        if (inChunkX + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX + 1, chunkY, chunkZ, lod));
        if (inChunkY + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX, chunkY + 1, chunkZ, lod));
        if (inChunkZ + (1 << lodSize) == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunkX, chunkY, chunkZ + 1, lod));
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;

        int minX = Math.max(0, (int) (min.x - position.x)), maxX = Math.min((int) (max.x - position.x), 1 << size);
        int minY = Math.max(0, (int) (min.y - position.y)), maxY = Math.min((int) (max.y - position.y), 1 << size);
        int minZ = Math.max(0, (int) (min.z - position.z)), maxZ = Math.min((int) (max.z - position.z), 1 << size);

        for (int x = minX; x < maxX; x++)
            for (int y = minY; y < maxY; y++)
                for (int z = minZ; z < maxZ; z++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) != 0) return true;
                }

        return false;
    }

    @Override
    public void offsetPosition(Vector3l position) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int mask = -(1 << breakPlaceSize);
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public Structure getStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16);
        byte[] uncompressedMaterial = new byte[4096];

        for (int bitsIndex = 0; bitsIndex < bitMap.length; bitsIndex++)
            for (int index = (bitsIndex << 6) + Long.numberOfTrailingZeros(bitMap[bitsIndex]),
                 end = bitsIndex + 1 << 6; index < end; index++) {
                if ((bitMap[bitsIndex] & 1L << index) == 0) continue;
                uncompressedMaterial[index] = STONE;
            }

        return new Structure(16, 16, 16, MaterialsData.getCompressedMaterials(4, uncompressedMaterial));
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        int breakPlaceSize = 1 << player.getInteractionHandler().getBreakPlaceSize();
        player.getParticleCollector().addBreakParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, material);
        player.getParticleCollector().addPlaceParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, material);
    }

    protected abstract void fillBitMap(long[] bitMap, int sideLength);

    private long[] getBitMap() {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        if (bitMap == null || size != breakPlaceSize) {
            long[] bitMap = new long[1 << Math.max(breakPlaceSize * 3 - 6, 1)];
            fillBitMap(bitMap, 1 << breakPlaceSize);
            this.bitMap = bitMap;
        }
        size = breakPlaceSize;
        return bitMap;
    }

    private int size = -1;
    private long[] bitMap;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    final byte material;
}
