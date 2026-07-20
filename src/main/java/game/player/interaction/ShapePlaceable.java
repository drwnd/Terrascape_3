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
import game.server.Chunk;
import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public abstract class ShapePlaceable implements Placeable {

/**
 * Creates a new ShapePlaceable instance.
 *
 * @param identifier parameter
 * @param material parameter
 */
    protected ShapePlaceable(ShaderIdentifier identifier, byte material) {
        this.shaderIdentifier = identifier;
        this.material = material;
        this.rotation = new StandAloneOptionSetting(null);
    }

/**
 * Creates a new ShapePlaceable instance.
 *
 * @param identifier parameter
 * @param material parameter
 * @param defaultRotation parameter
 */
    protected ShapePlaceable(ShaderIdentifier identifier, byte material, Option defaultRotation) {
        this.shaderIdentifier = identifier;
        this.material = material;
        this.rotation = new StandAloneOptionSetting(defaultRotation);
    }

/**
 * Copies with material.
 *
 * @param material parameter
 * @return result
 */
    public final ShapePlaceable copyWithMaterial(byte material) {
        ShapePlaceable placeable = copyWithMaterialUnique(material);
        placeable.invert.setValue(invert.value());
        placeable.rotation.setValue(rotation.value());
        placeable.setBitMap(bitMap);
        return placeable;
    }

/**
 * Returns the setting buttons.
 * @return result
 */
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

/**
 * Returns the small structure.
 * @return result
 */
    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        fillBitMap(bitMap, 16, true);
        return new Structure(4, material, bitMap);
    }

/**
 * Performs rotate forwards.
 */
    @Override
    public void rotateForwards() {
        if (rotation.value() == null) return;
        rotation.setValue(rotation.value().next());
        updateBitMap(false);
    }

/**
 * Performs rotate backwards.
 */
    @Override
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
            fillBitMap(bitMap, preferredSizePowOf2, false);
            this.bitMap = bitMap;
            this.settingsHash = settingsHash;
            this.preferredSize = preferredSize;
        }
        return this;
    }

/**
 * Sets bit map to full.
 * @return result
 */
    public ShapePlaceable setBitMapToFull() {
        int size = getPreferredSizePowOf2();
        long[] bitMap = new long[Math.max(1, size * size * size >> 6)];
        Arrays.fill(bitMap, -1L);
        setBitMap(bitMap);
        return this;
    }

/**
 * Performs delete.
 */
    public void delete() {
        AssetManager.delete(shaderIdentifier);
        glDeleteBuffers(buffer);
        bufferSize = -1;
    }

    public boolean isBitMapInvalid() {
        return isBitMapInValid(settingsHash(), getPreferredSize());
    }


/**
 * Returns the structure.
 * @return result
 */
    @Override
    public Structure getStructure() {
        int preferredSize = getPreferredSizePowOf2();
        int preferredSizeBits = Integer.numberOfTrailingZeros(preferredSize);

        return new Structure(getLengthX(), getLengthY(), getLengthZ(), preferredSizeBits, material, bitMap);
    }

/**
 * Performs intersects aabb.
 *
 * @param position parameter
 * @param min parameter
 * @param max X coordinate in local block coordinates
 * @return true if the condition holds
 */
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
    public void place(Vector3l position, int lod) {
        throw new UnsupportedOperationException("Delegated to RepeatPlaceable");
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        throw new UnsupportedOperationException("Delegated to RepeatPlaceable");
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        throw new UnsupportedOperationException("Delegated to RepeatPlaceable");
    }

    @Override
    public void spawnParticles(Vector3l position) {
        throw new UnsupportedOperationException("Delegated to RepeatPlaceable");
    }


    protected Option rotation() {
        return rotation.value();
    }

    protected abstract ShapePlaceable copyWithMaterialUnique(byte material);

    protected abstract ShapeSetting[] getSettings();

/**
 * Performs load settings.
 */
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

/**
 * Sets shader identifier.
 *
 * @param shaderIdentifier parameter
 */
    protected void setShaderIdentifier(ShaderIdentifier shaderIdentifier) {
        AssetManager.delete(this.shaderIdentifier);
        this.shaderIdentifier = shaderIdentifier;
    }


/**
 * Fills bit map.
 *
 * @param bitMap parameter
 * @param size parameter
 * @param forceSize parameter
 */
    private void fillBitMap(long[] bitMap, int size, boolean forceSize) {
        int lengthX = forceSize ? size : Math.min(getLengthX(), size);
        int lengthY = forceSize ? size : Math.min(getLengthY(), size);
        int lengthZ = forceSize ? size : Math.min(getLengthZ(), size);
        int numGroups = Math.max(1, size >> 4);
        int buffer = genBuffer(bitMap.length << 3);

        Shader shader = AssetManager.get(shaderIdentifier);
        shader.bind();
        for (ShapeSetting setting : settings) setting.setUniform(shader);
        shader.setUniform("size", lengthX, lengthY, lengthZ);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, buffer);
        glDispatchCompute(numGroups, numGroups, numGroups);

        glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, bitMap);
    }

/**
 * Performs gen buffer.
 *
 * @param size parameter
 * @return result
 */
    private int genBuffer(int size) {
        if (size == bufferSize) return buffer;
        glDeleteBuffers(buffer);
        buffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, size, GL_DYNAMIC_READ);
        return buffer;
    }

    private boolean isBitMapInValid(int settingsHash, int preferredSize) {
        return bitMap == null || settingsHash != this.settingsHash || this.preferredSize != preferredSize;
    }

    private int settingsHash() {
        return Objects.hash((Object[]) settings);
    }

/**
 * Sets bit map.
 *
 * @param bitMap parameter
 */
    private void setBitMap(long[] bitMap) {
        this.bitMap = bitMap;
        settingsHash = settingsHash();
        preferredSize = getPreferredSize();
    }


    private int buffer, bufferSize = -1;
    private int settingsHash, preferredSize;
    private long[] bitMap;
    private final byte material;
    private ShapeSetting[] settings;

    final ToggleSetting invert = new StandAloneToggleSetting(false);
    private final StandAloneOptionSetting rotation;
    private ShaderIdentifier shaderIdentifier;
}
