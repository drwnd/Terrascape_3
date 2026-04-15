package game.player.interaction;

import core.assets.AssetManager;
import core.assets.identifiers.ShaderIdentifier;
import core.renderables.UiButton;
import core.rendering_api.shaders.Shader;
import core.settings.ToggleSetting;
import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneOptionSetting;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public abstract class ShapePlaceable implements Placeable {

    protected ShapePlaceable(ShaderIdentifier identifier, byte material) {
        this.shaderIdentifier = identifier;
        this.material = material;
        this.rotation = new StandAloneOptionSetting(null);
    }

    protected ShapePlaceable(ShaderIdentifier identifier, byte material, Option defaultRotation) {
        this.shaderIdentifier = identifier;
        this.material = material;
        this.rotation = new StandAloneOptionSetting(defaultRotation);
    }

    public final ShapePlaceable copyWithMaterial(byte material) {
        ShapePlaceable placeable = copyWithMaterialUnique(material);
        placeable.invert.setValue(invert.value());
        placeable.rotation.setValue(rotation.value());
        placeable.setBitMap(bitMap);
        return placeable;
    }

    public final List<UiButton> getSettingButtons() {
        ArrayList<UiButton> settingElements = new ArrayList<>();

        for (ShapeSetting setting : settings) settingElements.add(setting.getSettingButton());

        for (UiButton element : settingElements) element.setScaleWithGuiSize(false);
        return settingElements;
    }

    public byte getMaterial() {
        return material;
    }

    public long[] getBitMap() {
        return bitMap;
    }

    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16);
        return new Structure(4, material, bitMap);
    }

    public void rotateForwards() {
        if (rotation.value() == null) return;
        rotation.setValue(rotation.value().next());
        updateBitMap(false);
    }

    public void rotateBackwards() {
        if (rotation.value() == null) return;
        rotation.setValue(rotation.value().previous());
        updateBitMap(false);
    }

    /**
     * Only call from main thread!
     */
    public ShapePlaceable updateBitMap(boolean force) {
        int preferredSize = getPreferredSize(), preferredSizePowOf2 = MathUtils.nextLargestPowOf2(preferredSize);
        int settingsHash = settingsHash();
        if (force || isBitMapInValid(settingsHash, preferredSize)) {
            long[] bitMap = new long[Math.max(preferredSizePowOf2 * preferredSizePowOf2 * preferredSizePowOf2 >> 6, 1)];
            fillBitMap(bitMap, preferredSizePowOf2);
            this.bitMap = bitMap;
            this.settingsHash = settingsHash;
            this.preferredSize = preferredSize;
        }
        return this;
    }

    public ShapePlaceable setBitMapToFull() {
        int size = getPreferredSizePowOf2();
        long[] bitMap = new long[Math.max(1, size * size * size >> 6)];
        Arrays.fill(bitMap, -1L);
        setBitMap(bitMap);
        return this;
    }

    public void delete() {
        AssetManager.delete(shaderIdentifier);
        glDeleteBuffers(buffer);
        bufferSize = -1;
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
        player.getParticleCollector().addBreakPlaceParticleEffect(position.x, position.y, position.z, preferredSize, material, bitMap);
    }


    protected Option rotation() {
        return rotation.value();
    }

    protected abstract ShapePlaceable copyWithMaterialUnique(byte material);

    protected abstract ShapeSetting[] getSettings();

    protected void loadSettings() {
        ShapeSetting[] baseSettings = getSettings();

        if (rotation.value() == null) {
            settings = new ShapeSetting[baseSettings.length + 1];
            System.arraycopy(baseSettings, 0, settings, 1, baseSettings.length);
            settings[0] = new ShapeSetting(invert, UiMessages.INVERT_PLACEABLE, "invert");
        } else {
            settings = new ShapeSetting[baseSettings.length + 2];
            System.arraycopy(baseSettings, 0, settings, 2, baseSettings.length);
            settings[0] = new ShapeSetting(invert, UiMessages.INVERT_PLACEABLE, "invert");
            settings[1] = new ShapeSetting(rotation, UiMessages.SHAPE_ROTATION, "rotation");
        }
    }

    protected void setShaderIdentifier(ShaderIdentifier shaderIdentifier) {
        AssetManager.delete(this.shaderIdentifier);
        this.shaderIdentifier = shaderIdentifier;
    }


    private void fillBitMap(long[] bitMap, int preferredSizePowOf2) {
        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        int numGroups = preferredSizePowOf2 >> 2;
        int buffer = genBuffer(bitMap.length << 3);

        Shader shader = AssetManager.get(shaderIdentifier);
        shader.bind();
        for (ShapeSetting setting : settings) setting.setUniform(shader);
        shader.setUniform("size", lengthX, lengthY, lengthZ);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, buffer);
        glDispatchCompute(numGroups, numGroups, numGroups);

        glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, bitMap);
    }

    private int genBuffer(int size) {
        if (size == bufferSize) return buffer;
        glDeleteBuffers(buffer);
        buffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, size, GL_DYNAMIC_READ);
        return buffer;
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

    private boolean isBitMapInValid(int settingsHash, int preferredSize) {
        return bitMap == null || settingsHash != this.settingsHash || this.preferredSize != preferredSize;
    }

    private int settingsHash() {
        return Objects.hash((Object[]) settings);
    }

    private void setBitMap(long[] bitMap) {
        this.bitMap = bitMap;
        settingsHash = settingsHash();
        preferredSize = getPreferredSize();
    }


    private int buffer, bufferSize = -1;
    private int settingsHash, preferredSize;
    private long[] bitMap;
    private final byte material;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private ShapeSetting[] settings;

    final ToggleSetting invert = new StandAloneToggleSetting(false);
    private final StandAloneOptionSetting rotation;
    private ShaderIdentifier shaderIdentifier;
}
