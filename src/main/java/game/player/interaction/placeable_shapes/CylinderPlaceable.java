package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation3Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class CylinderPlaceable extends ShapePlaceable {

    /**
     * Initializes the cylinder placeable with a specific material and default rotation.
     *
     * @param material the material of the cylinder
     */
    public CylinderPlaceable(byte material) {
        super(ComputeShaders.CYLINDER, material, Rotation3Way.ROTATION_2);
        loadSettings();
    }

    /**
     * Saves the cylinder placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 5);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(thickness.value());
        saver.saveInt(height.value());
        saver.saveFloat(exponent.value());
    }

    public static CylinderPlaceable load(Saver<?> saver) {
        CylinderPlaceable placeable = new CylinderPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    /**
     * Returns the shape settings for the cylinder placeable.
     *
     * @return an array of {@code ShapeSetting}
     */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(height, UiMessages.HEIGHT, "height"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    /**
     * Returns the length of the cylinder in the X direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthX() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_2 -> radius.value() * 2;
            case Rotation3Way.ROTATION_1 -> height.value();
        };
    }

    /**
     * Returns the length of the cylinder in the Y direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthY() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_1 -> radius.value() * 2;
            case Rotation3Way.ROTATION_2 -> height.value();
        };
    }

    /**
     * Returns the length of the cylinder in the Z direction based on its rotation.
     *
     * @return the length in blocks at the relevant LOD
     */
    @Override
    public int getLengthZ() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3 -> height.value();
            case Rotation3Way.ROTATION_2, Rotation3Way.ROTATION_1 -> radius.value() * 2;
        };
    }

    /**
     * Creates a copy of this cylinder placeable with a different material.
     *
     * @param material the new material
     * @return a new {@code CylinderPlaceable} instance
     */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CylinderPlaceable copy = new CylinderPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.thickness.setValue(thickness.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        return copy;
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
