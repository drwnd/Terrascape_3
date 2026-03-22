package game.player.interaction;

import core.renderables.Toggle;
import core.renderables.UiBackgroundElement;
import core.settings.ToggleSetting;
import core.settings.stand_alones.StandAloneToggleSetting;
import core.utils.Vector3l;

import game.language.UiMessages;
import game.player.Player;
import game.server.Chunk;
import game.server.Game;
import game.server.MaterialsData;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static game.utils.Constants.*;

public abstract class ShapePlaceable implements Placeable {

    protected ShapePlaceable(byte material) {
        this.material = material;
    }

    public final ShapePlaceable copyWithMaterial(byte material) {
        ShapePlaceable placeable = copyWithMaterialUnique(material);
        placeable.invert.setValue(invert.value());
        return placeable;
    }

    public final List<UiBackgroundElement> settings() {
        Vector2f zero = new Vector2f(0.0F, 0.0F);
        ArrayList<UiBackgroundElement> settingElements = new ArrayList<>();

        settingElements.add(new Toggle(zero, zero, invert, UiMessages.INVERT_PLACEABLE, true));
        settingElements.addAll(uniqueSettings());

        for (UiBackgroundElement element : settingElements) element.setScaleWithGuiSize(false);
        return settingElements;
    }

    public byte getMaterial() {
        return material;
    }

    public long[] getBitMap() {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        if (isBitMapInValid(breakPlaceSize)) {
            long[] bitMap = new long[1 << Math.max(breakPlaceSize * 3 - 6, 1)];
            fillBitMap(bitMap, 1 << breakPlaceSize);
            this.bitMap = bitMap;
            if (invert.value()) for (int index = 0; index < bitMap.length; index++) bitMap[index] = ~bitMap[index];
        }
        size = breakPlaceSize;
        return bitMap;
    }

    public Structure getPlaceBreakSizedStructure() {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int sideLength = 1 << breakPlaceSize;
        long[] bitMap = getBitMap();

        byte[] uncompressedMaterial = new byte[1 << breakPlaceSize * 3];
        populateUncompressedMaterials(bitMap, uncompressedMaterial);
        return new Structure(sideLength, sideLength, sideLength, MaterialsData.getCompressedMaterials(breakPlaceSize, uncompressedMaterial));
    }


    @Override
    public void place(Vector3l position, int lod) {
        int breakPlaceSize = 1 << Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int mask = -breakPlaceSize;
        if (Long.numberOfTrailingZeros(position.x & mask) < lod
                || Long.numberOfTrailingZeros(position.y & mask) < lod
                || Long.numberOfTrailingZeros(position.z & mask) < lod) return;

        long chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(position.x + breakPlaceSize >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(position.y + breakPlaceSize >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + breakPlaceSize >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (long chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (long chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod), position);
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;
        long[] bitMap = getBitMap();

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
        int breakPlaceAlign = Game.getPlayer().getInteractionHandler().getBreakPlaceAlign();
        int mask = -(1 << breakPlaceAlign);
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public Structure getStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16);
        if (invert.value()) for (int index = 0; index < 64; index++) bitMap[index] = ~bitMap[index];
        byte[] uncompressedMaterial = new byte[4096];
        populateUncompressedMaterials(bitMap, uncompressedMaterial);
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
        player.getParticleCollector().addBreakPlaceParticleEffect(position.x, position.y, position.z, breakPlaceSize, material, getBitMap());
    }

    protected abstract void fillBitMap(long[] bitMap, int sideLength);

    protected abstract List<UiBackgroundElement> uniqueSettings();

    protected abstract ShapePlaceable copyWithMaterialUnique(byte material);

    protected boolean isBitMapInValid(int breakPlaceSize) {
        return bitMap == null || size != breakPlaceSize;
    }

    private void placeInChunk(Chunk chunk, Vector3l position) {
        int lod = chunk.LOD;
        long[] bitMap = getBitMap();
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();

        int inChunkX = (int) (position.x - (chunk.X << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkY = (int) (position.y - (chunk.Y << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkZ = (int) (position.z - (chunk.Z << CHUNK_SIZE_BITS + lod)) >> lod;
        int lodSize = Math.max(0, breakPlaceSize - lod);

        int length = 1 << lodSize;
        int align = Game.getPlayer().getInteractionHandler().getBreakPlaceAlign();
        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, material, 1, 1, 1, length, bitMap, lod, align);

        World world = Game.getWorld();
        affectedChunks.add(chunk);
        if (inChunkX <= 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, lod));
        if (inChunkY <= 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, lod));
        if (inChunkZ <= 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, lod));
        if (inChunkX + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, lod));
        if (inChunkY + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, lod));
        if (inChunkZ + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, lod));
    }

    private void populateUncompressedMaterials(long[] bitMap, byte[] uncompressedMaterials) {
        for (int bitsIndex = 0; bitsIndex < bitMap.length; bitsIndex++)
            for (int index = (bitsIndex << 6) + Long.numberOfTrailingZeros(bitMap[bitsIndex]),
                 end = Math.min(bitsIndex + 1 << 6, uncompressedMaterials.length); index < end; index++) {
                if ((bitMap[bitsIndex] & 1L << index) == 0) continue;
                uncompressedMaterials[index] = material;
            }
    }

    private int size = -1;
    private long[] bitMap;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    final byte material;

    final ToggleSetting invert = new StandAloneToggleSetting(false);
}
