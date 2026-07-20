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
 * Creates a new CubePlaceable instance.
 *
 * @param material parameter
 */
    public CubePlaceable(byte material) {
        super(ComputeShaders.CUBE, material);
        loadSettings();
    }

/**
 * Performs save.
 *
 * @param saver parameter
 */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(getMaterial());
        saver.saveInt(sizeReduction.value());
        saver.saveInt(thickness.value());
    }

/**
 * Performs load.
 *
 * @param saver parameter
 * @return result
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
 * Copies with material unique.
 *
 * @param material parameter
 * @return result
 */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CubePlaceable copy = new CubePlaceable(material);
        copy.sizeReduction.setValue(sizeReduction.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

/**
 * Returns the settings.
 * @return array result
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
