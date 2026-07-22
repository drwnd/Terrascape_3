package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation8Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class OutsideArcPlaceable extends ShapePlaceable {

    /**
     * Initializes the outside arc placeable with a specific material and default rotation.
     *
     * @param material the material of the outside arc
     */
    public OutsideArcPlaceable(byte material) {
        super(ComputeShaders.OUTSIDE_ARC, material, Rotation8Way.ROTATION_1);
        loadSettings();
    }

    /**
     * Saves the outside arc placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 17);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(exponent.value());
    }

    public static OutsideArcPlaceable load(Saver<?> saver) {
        OutsideArcPlaceable placeable = new OutsideArcPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    /**
     * Creates a copy of this outside arc placeable with a different material.
     *
     * @param material the new material
     * @return a new {@code OutsideArcPlaceable} instance
     */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        OutsideArcPlaceable copy = new OutsideArcPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    /**
     * Returns the shape settings for the outside arc placeable.
     *
     * @return an array of {@code ShapeSetting}
     */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    @Override
    public int getLengthX() {
        return radius.value();
    }

    @Override
    public int getLengthY() {
        return radius.value();
    }

    @Override
    public int getLengthZ() {
        return radius.value();
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.1F, 16.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
