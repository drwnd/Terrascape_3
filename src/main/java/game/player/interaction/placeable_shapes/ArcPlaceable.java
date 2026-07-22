package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation12Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class ArcPlaceable extends ShapePlaceable {

    /**
     * Initializes the arc placeable with a specific material and default rotation.
     *
     * @param material the material of the arc
     */
    public ArcPlaceable(byte material) {
        super(ComputeShaders.ARC, material, Rotation12Way.ROTATION_01);
        loadSettings();
    }

    /**
     * Saves the arc placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 15);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(height.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(exponent.value());
    }

    /**
     * Loads an arc placeable from a saver.
     *
     * @param saver the saver to load from
     * @return the loaded arc placeable
     */
    public static ArcPlaceable load(Saver<?> saver) {
        ArcPlaceable placeable = new ArcPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    /**
     * Creates a copy of this arc placeable with a different material.
     *
     * @param material the new material
     * @return a new {@code ArcPlaceable} instance
     */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        ArcPlaceable copy = new ArcPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    /**
     * Returns the shape settings for the arc placeable.
     *
     * @return an array of {@code ShapeSetting}
     */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(height, UiMessages.HEIGHT, "height"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    /**
     * Returns the length of the arc in the X direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthX() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_01, ROTATION_03, ROTATION_09, ROTATION_11 -> height.value();
            case ROTATION_02, ROTATION_04, ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08, ROTATION_10, ROTATION_12 -> radius.value();
        };
    }

    /**
     * Returns the length of the arc in the Y direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthY() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08 -> height.value();
            case ROTATION_01, ROTATION_02, ROTATION_03, ROTATION_04, ROTATION_09, ROTATION_10, ROTATION_11, ROTATION_12 -> radius.value();
        };
    }

    /**
     * Returns the length of the arc in the Z direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthZ() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_02, ROTATION_04, ROTATION_10, ROTATION_12 -> height.value();
            case ROTATION_01, ROTATION_03, ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08, ROTATION_09, ROTATION_11 -> radius.value();
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 8);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.1F, 16.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
