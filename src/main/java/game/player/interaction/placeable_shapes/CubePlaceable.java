package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class CubePlaceable extends ShapePlaceable {

    /**
     * Initializes the cube placeable with a specific material.
     *
     * @param material the material of the cube
     */
    public CubePlaceable(byte material) {
        super(ComputeShaders.CUBE, material);
        loadSettings();
    }

    /**
     * Saves the cube placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(getMaterial());
        saver.saveInt(sizeReduction.value());
        saver.saveInt(thickness.value());
    }

    /**
     * Loads a cube placeable from a saver.
     *
     * @param saver the saver to load from
     * @return the loaded cube placeable
     */
    public static CubePlaceable load(Saver<?> saver) {
        CubePlaceable placeable = new CubePlaceable(saver.loadByte());
        placeable.sizeReduction.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public int getLengthX() {
        return super.getLengthX() - 2 * sizeReduction.value();
    }

    @Override
    public int getLengthY() {
        return super.getLengthY() - 2 * sizeReduction.value();
    }

    @Override
    public int getLengthZ() {
        return super.getLengthZ() - 2 * sizeReduction.value();
    }

    /**
     * Creates a copy of this cube placeable with a different material.
     *
     * @param material the new material
     * @return a new {@code CubePlaceable} instance
     */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CubePlaceable copy = new CubePlaceable(material);
        copy.sizeReduction.setValue(sizeReduction.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    /**
     * Returns the shape settings for the cube placeable.
     *
     * @return an array of {@code ShapeSetting}
     */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(sizeReduction, UiMessages.SIZE_REDUCTION, "sizeReduction"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness")
        };
    }

    @Override
    public Structure getSmallStructure() {
        return new Structure(4, getMaterial());
    }

    private final StandAloneIntSetting sizeReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
