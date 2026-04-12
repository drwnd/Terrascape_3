package game.player.interaction.placeable_shapes;

import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation24Way;
import game.player.interaction.ShapeSetting;

import static game.utils.Constants.CHUNK_SIZE;

public final class InsideStairPlaceable extends RotatableShapePlaceable implements ConerStairPlaceable {

    public InsideStairPlaceable(byte material) {
        super(material, Rotation24Way.ROTATION_17);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 9);
        saver.saveByte(getMaterial());
        saver.saveInt(stepHeight.value());
        saver.saveInt(heightOffset.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(slope.value());
    }

    public static InsideStairPlaceable load(Saver<?> saver) {
        InsideStairPlaceable placeable = new InsideStairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        InsideStairPlaceable copy = new InsideStairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.thickness.setValue(thickness.value());
        copy.slope.setValue(slope.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int outerThreshold = (sideLength + heightOffset.value()) / stepHeight.value();
        int innerThreshold = Math.min(outerThreshold - 1, outerThreshold - thickness.value() / stepHeight.value());
        fillBitMap(bitMap, sideLength, outerThreshold, innerThreshold);
    }

    @Override
    protected ShapeSetting[] getSettingsRotatable() {
        return new ShapeSetting[]{
                new ShapeSetting(stepHeight, UiMessages.STEP_HEIGHT, "stepHeight"),
                new ShapeSetting(heightOffset, UiMessages.HEIGHT, "heightOffset"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(slope, UiMessages.SLOPE, "slope")
        };
    }

    @Override
    public Option getRotation() {
        return rotation();
    }

    public boolean isInside(int outerThreshold, int innerThreshold, int a, int b, int c) {
        float distance = a / stepHeight.value() * slope.value() + Math.min(b, c) / stepHeight.value();
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 1.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
