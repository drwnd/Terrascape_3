package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation24Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;

import static game.utils.Constants.CHUNK_SIZE;

public final class InsideStairPlaceable extends ShapePlaceable {

    /**
     * Constructs a new InsideStairPlaceable with the specified material.
     * @param material the material to be used for the stair
     */
    public InsideStairPlaceable(byte material) {
        super(ComputeShaders.INSIDE_STAIR, material, Rotation24Way.ROTATION_17);
        loadSettings();
    }

    /**
     * Saves the state of this placeable using the provided saver.
     * @param saver the saver used for data persistence
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 9);
        saver.saveByte(getMaterial());
        saver.saveInt(stepHeight.value());
        saver.saveInt(heightOffset.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(slope.value());
    }

    /**
     * Loads an InsideStairPlaceable state from the provided saver.
     * @param saver the saver to load from
     * @return a new InsideStairPlaceable instance with loaded state
     */
    public static InsideStairPlaceable load(Saver<?> saver) {
        InsideStairPlaceable placeable = new InsideStairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    /**
     * Creates a copy of this placeable with a different material.
     * @param material the material for the new copy
     * @return a new InsideStairPlaceable instance
     */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        InsideStairPlaceable copy = new InsideStairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.thickness.setValue(thickness.value());
        copy.slope.setValue(slope.value());
        return copy;
    }

    /**
     * Returns the configurable settings for this shape.
     * @return an array of {@link ShapeSetting} for the stair properties
     */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(stepHeight, UiMessages.STEP_HEIGHT, "stepHeight"),
                new ShapeSetting(heightOffset, UiMessages.HEIGHT, "heightOffset"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(slope, UiMessages.SLOPE, "slope")
        };
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 1.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
