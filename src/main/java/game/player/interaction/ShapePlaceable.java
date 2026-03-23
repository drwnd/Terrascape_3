package game.player.interaction;

import core.renderables.Toggle;
import core.renderables.UiBackgroundElement;
import core.settings.ToggleSetting;
import core.settings.stand_alones.StandAloneToggleSetting;
import core.utils.MathUtils;
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
import game.settings.IntSettings;
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
        int preferredSize = MathUtils.nextLargestPowOf2(getPreferredSize());
        if (isBitMapInValid(preferredSize)) {
            long[] bitMap = new long[Math.max(preferredSize * preferredSize * preferredSize >> 6, 1)];
            fillBitMap(bitMap, preferredSize);
            this.bitMap = bitMap;
            if (invert.value()) for (int index = 0; index < bitMap.length; index++) bitMap[index] = ~bitMap[index];
        }
        this.preferredSize = preferredSize;
        return bitMap;
    }

    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16);
        return new Structure(4, material, bitMap);
    }


    @Override
    public void place(Vector3l position, int lod) {
        int preferredSize = MathUtils.nextLargestPowOf2(getPreferredSize());
        int mask = -preferredSize;
        if (Long.numberOfTrailingZeros(position.x & mask) < lod
                || Long.numberOfTrailingZeros(position.y & mask) < lod
                || Long.numberOfTrailingZeros(position.z & mask) < lod) return;

        long chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(position.x + preferredSize >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(position.y + preferredSize >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + preferredSize >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
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
        int preferredSize = getPreferredSize();

        int minX = Math.max(0, (int) (min.x - position.x)), maxX = Math.min((int) (max.x - position.x), preferredSize);
        int minY = Math.max(0, (int) (min.y - position.y)), maxY = Math.min((int) (max.y - position.y), preferredSize);
        int minZ = Math.max(0, (int) (min.z - position.z)), maxZ = Math.min((int) (max.z - position.z), preferredSize);

        for (int x = minX; x < maxX; x++)
            for (int y = minY; y < maxY; y++)
                for (int z = minZ; z < maxZ; z++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) != 0) return true;
                }

        return false;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        int breakPlaceAlign = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        int mask = -breakPlaceAlign;

        RepeatPlaceable.offsetPositionFromGround(position, targetedSide, getPreferredSize());
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public Structure getStructure() {
        int preferredSize = MathUtils.nextLargestPowOf2(getPreferredSize());
        int preferredSizeBits = Integer.numberOfTrailingZeros(preferredSize);
        long[] bitMap = getBitMap();

        return new Structure(preferredSizeBits, material, bitMap);
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        int preferredSize = MathUtils.nextLargestPowOf2(getPreferredSize());
        player.getParticleCollector().addBreakPlaceParticleEffect(position.x, position.y, position.z, preferredSize, material, getBitMap());
    }

    protected abstract void fillBitMap(long[] bitMap, int sideLength);

    protected abstract List<UiBackgroundElement> uniqueSettings();

    protected abstract ShapePlaceable copyWithMaterialUnique(byte material);

    protected boolean isBitMapInValid(int preferredSize) {
        return bitMap == null || this.preferredSize != preferredSize;
    }

    private void placeInChunk(Chunk chunk, Vector3l position) {
        int lod = chunk.LOD;
        long[] bitMap = getBitMap();
        int preferredSizeBits = Integer.numberOfTrailingZeros(MathUtils.nextLargestPowOf2(getPreferredSize()));

        int inChunkX = (int) (position.x - (chunk.X << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkY = (int) (position.y - (chunk.Y << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkZ = (int) (position.z - (chunk.Z << CHUNK_SIZE_BITS + lod)) >> lod;
        int lodSize = Math.max(0, preferredSizeBits - lod);

        int length = 1 << lodSize;
        int align = IntSettings.BREAK_PLACE_ALIGN.value();
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


    private int preferredSize = -1;
    private long[] bitMap;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    final byte material;

    final ToggleSetting invert = new StandAloneToggleSetting(false);
}
