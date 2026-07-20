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
 * Creates a new ArcPlaceable instance.
 *
 * @param material parameter
 */
    public ArcPlaceable(byte material) {
        super(ComputeShaders.ARC, material, Rotation12Way.ROTATION_01);
        loadSettings();
    }

/**
 * Performs save.
 *
 * @param saver parameter
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
 * Performs load.
 *
 * @param saver parameter
 * @return result
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
 * Copies with material unique.
 *
 * @param material parameter
 * @return result
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
 * Returns the settings.
 * @return array result
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
 * Returns the length x.
 * @return result
 */
    @Override
    public int getLengthX() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_01, ROTATION_03, ROTATION_09, ROTATION_11 -> height.value();
            case ROTATION_02, ROTATION_04, ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08, ROTATION_10, ROTATION_12 -> radius.value();
        };
    }

/**
 * Returns the length y.
 * @return result
 */
    @Override
    public int getLengthY() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08 -> height.value();
            case ROTATION_01, ROTATION_02, ROTATION_03, ROTATION_04, ROTATION_09, ROTATION_10, ROTATION_11, ROTATION_12 -> radius.value();
        };
    }

/**
 * Returns the length z.
 * @return result
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
