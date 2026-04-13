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

public final class StairPlaceable extends ShapePlaceable {

    public StairPlaceable(byte material) {
        super(ComputeShaders.STAIR, material, Rotation24Way.ROTATION_01);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 6);
        saver.saveByte(getMaterial());
        saver.saveInt(stepHeight.value());
        saver.saveInt(heightOffset.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(slope.value());
    }

    public static StairPlaceable load(Saver<?> saver) {
        StairPlaceable placeable = new StairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        StairPlaceable copy = new StairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.slope.setValue(slope.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

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
