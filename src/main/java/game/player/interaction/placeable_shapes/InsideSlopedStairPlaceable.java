package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation24Way;

import org.joml.Vector2f;

import java.util.List;

import static game.utils.Constants.CHUNK_SIZE;

public final class InsideSlopedStairPlaceable extends RotatableShapePlaceable implements ConerStairPlaceable {

    public InsideSlopedStairPlaceable(byte material) {
        super(material, Rotation24Way.ROTATION_17);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 11);
        saver.saveByte(((InsideSlopedStairPlaceable) placeable).getMaterial());
        saver.saveInt(((InsideSlopedStairPlaceable) placeable).stepHeight.value());
        saver.saveInt(((InsideSlopedStairPlaceable) placeable).heightOffset.value());
        saver.saveInt(((InsideSlopedStairPlaceable) placeable).thickness.value());
        saver.saveFloat(((InsideSlopedStairPlaceable) placeable).slope.value());
    }

    public static InsideSlopedStairPlaceable load(Saver<?> saver) {
        InsideSlopedStairPlaceable placeable = new InsideSlopedStairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        InsideSlopedStairPlaceable copy = new InsideSlopedStairPlaceable(material);
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
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, stepHeight, UiMessages.STEP_HEIGHT, true),
                new Slider<>(zero, zero, heightOffset, UiMessages.HEIGHT, true),
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true),
                new Slider<>(zero, zero, slope, UiMessages.SLOPE, true)
        );
    }

    @Override
    public Option getRotation() {
        return rotation;
    }

    public boolean isInside(int outerThreshold, int innerThreshold, int a, int b, int c) {
        float distance = a / stepHeight.value() * slope.value() + Math.min(b, c) / stepHeight.value();
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
