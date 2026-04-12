package game.player.interaction;

import core.renderables.UiButton;
import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneOptionSetting;
import core.settings.stand_alones.StandAloneToggleSetting;
import core.utils.MathUtils;
import core.utils.Saver;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static game.utils.Constants.*;

public abstract class ShapePlaceable implements Placeable {

    private ShapePlaceable(byte material, Option defaultRotation, ShapeSetting... settings) {
        this.material = material;
        this.rotation = defaultRotation;

        this.settings = new ShapeSetting[settings.length + 2];
        System.arraycopy(settings, 0, this.settings, 2, settings.length);
        this.settings[0] = new ShapeSetting(new StandAloneToggleSetting(false), UiMessages.INVERT_PLACEABLE, "invert");
        this.settings[1] = new ShapeSetting(new StandAloneOptionSetting(defaultRotation), UiMessages.ROTATE_SHAPE_BACKWARD, "rotation"); // TODO UiMessage for rotation
    }

    private ShapePlaceable(byte material, ShapeSetting... settings) {
        this.material = material;
        this.rotation = Rotation1Way.ROTATION_1;

        this.settings = new ShapeSetting[settings.length + 1];
        System.arraycopy(settings, 0, this.settings, 1, settings.length);
        this.settings[0] = new ShapeSetting(new StandAloneToggleSetting(false), UiMessages.INVERT_PLACEABLE, "invert");
    }

    protected ShapePlaceable(ShapeSetting[] settings, byte material, Option rotation) {
        this.material = material;
        this.settings = settings;
        this.rotation = rotation;
    }


    public ShapePlaceable copyWithMaterial(byte material) {
        ShapeSetting[] settingsCopy = new ShapeSetting[settings.length];
        for (int index = 0; index < settings.length; index++)
            settingsCopy[index] = settings[index].copy();
        ShapePlaceable placeable = newInstance(settingsCopy, material, rotation);

        placeable.bitMap = getBitMap();
        placeable.settingsHash = placeable.settingsHash();
        placeable.preferredSize = placeable.getPreferredSize();
        return placeable;
    }

    public List<UiButton> getSettingsButtons() {
        ArrayList<UiButton> settingElements = new ArrayList<>();
        for (ShapeSetting shapeSetting : settings) settingElements.add(shapeSetting.getSettingButton());
        for (UiButton element : settingElements) element.setScaleWithGuiSize(false);
        return settingElements;
    }

    public byte getMaterial() {
        return material;
    }

    public long[] getBitMap() {
        int preferredSize = getPreferredSize(), preferredSizePowOf2 = MathUtils.nextLargestPowOf2(preferredSize);
        int settingsHash = settingsHash();
        if (isBitMapInValid(settingsHash, preferredSize)) {
            long[] bitMap = new long[Math.max(preferredSizePowOf2 * preferredSizePowOf2 * preferredSizePowOf2 >> 6, 1)];
            fillBitMap(bitMap, getPreferredSize());
            this.bitMap = bitMap;
        }
        this.settingsHash = settingsHash;
        this.preferredSize = preferredSize;
        return bitMap;
    }

    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16);
        return new Structure(4, material, bitMap);
    }

    public void rotateForwards() {
        rotation = rotation.next();
    }

    public void rotateBackwards() {
        rotation = rotation.previous();
    }


    @Override
    public void place(Vector3l position, int lod) {
        if (Long.numberOfTrailingZeros(position.x & -MathUtils.nextLargestPowOf2(getLengthX())) < lod
                || Long.numberOfTrailingZeros(position.y & -MathUtils.nextLargestPowOf2(getLengthY())) < lod
                || Long.numberOfTrailingZeros(position.z & -MathUtils.nextLargestPowOf2(getLengthZ())) < lod) return;

        long chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(position.x + getLengthX() - 1 >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(position.y + getLengthY() - 1 >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + getLengthZ() - 1 >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
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

        RepeatPlaceable.offsetPositionFromGround(position, targetedSide, getLengthX(), getLengthY(), getLengthZ());
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public Structure getStructure() {
        int preferredSize = getPreferredSizePowOf2();
        int preferredSizeBits = Integer.numberOfTrailingZeros(preferredSize);
        long[] bitMap = getBitMap();

        return new Structure(getLengthX(), getLengthY(), getLengthZ(), preferredSizeBits, material, bitMap);
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        int preferredSize = getPreferredSizePowOf2();
        player.getParticleCollector().addBreakPlaceParticleEffect(position.x, position.y, position.z, preferredSize, material, getBitMap());
    }

    @Override
    public void save(Saver<?> saver) {

    }

    public static ShapePlaceable load(Saver<?> saver, byte identifier) {
        return null;
    }


    private void fillBitMap(long[] bitMap, int sideLength) {
        // TODO compute shader stuff
    }


    protected abstract ShapePlaceable newInstance(ShapeSetting[] settings, byte material, Option rotation);


    private boolean isBitMapInValid(int settingsHash, int preferredSize) {
        return bitMap == null || settingsHash != this.settingsHash || this.preferredSize != preferredSize;
    }

    private int settingsHash() {
        return Objects.hash((Object[]) settings);
    }

    private void placeInChunk(Chunk chunk, Vector3l position) {
        int lod = chunk.LOD;
        int preferredSizeBits = Integer.numberOfTrailingZeros(getPreferredSizePowOf2());

        int inChunkX = (int) (position.x - (chunk.X << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkY = (int) (position.y - (chunk.Y << CHUNK_SIZE_BITS + lod)) >> lod;
        int inChunkZ = (int) (position.z - (chunk.Z << CHUNK_SIZE_BITS + lod)) >> lod;
        int lodSize = Math.max(0, preferredSizeBits - lod);

        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, lod, this);

        World world = Game.getWorld();
        affectedChunks.add(chunk);
        if (inChunkX <= 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, lod));
        if (inChunkY <= 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, lod));
        if (inChunkZ <= 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, lod));
        if (inChunkX + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, lod));
        if (inChunkY + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, lod));
        if (inChunkZ + (1 << lodSize) >= CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, lod));
    }


    private int settingsHash, preferredSize;
    private long[] bitMap;
    final byte material;

    private final ShapeSetting[] settings;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();

    private Option rotation;
}
