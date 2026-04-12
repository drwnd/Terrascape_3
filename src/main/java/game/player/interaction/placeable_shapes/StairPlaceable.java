package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation24Way;
import game.player.interaction.ShapeSetting;
import game.server.MaterialsData;

import static game.utils.Constants.CHUNK_SIZE;

public final class StairPlaceable extends RotatableShapePlaceable {

    public StairPlaceable(byte material) {
        super(material, Rotation24Way.ROTATION_01);
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
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        StairPlaceable copy = new StairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.slope.setValue(slope.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int outerThreshold = (sideLength + heightOffset.value()) / stepHeight.value();
        int innerThreshold = Math.min(outerThreshold - 1, outerThreshold - thickness.value() / stepHeight.value());

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
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

    private boolean isInside(int x, int y, int z, int sideLength, int outerThreshold, int innerThreshold) {
        int invert = sideLength - 1;
        return switch ((Rotation24Way) rotation) {
            case Rotation24Way.ROTATION_01 -> isInside(outerThreshold, innerThreshold, z, y);
            case Rotation24Way.ROTATION_02 -> isInside(outerThreshold, innerThreshold, x, y);
            case Rotation24Way.ROTATION_03 -> isInside(outerThreshold, innerThreshold, invert - z, y);
            case Rotation24Way.ROTATION_04 -> isInside(outerThreshold, innerThreshold, invert - x, y);

            case Rotation24Way.ROTATION_05 -> isInside(outerThreshold, innerThreshold, x, z);
            case Rotation24Way.ROTATION_06 -> isInside(outerThreshold, innerThreshold, invert - z, x);
            case Rotation24Way.ROTATION_07 -> isInside(outerThreshold, innerThreshold, invert - x, invert - z);
            case Rotation24Way.ROTATION_08 -> isInside(outerThreshold, innerThreshold, invert - x, z);

            case Rotation24Way.ROTATION_09 -> isInside(outerThreshold, innerThreshold, z, invert - y);
            case Rotation24Way.ROTATION_10 -> isInside(outerThreshold, innerThreshold, x, invert - y);
            case Rotation24Way.ROTATION_11 -> isInside(outerThreshold, innerThreshold, invert - z, invert - y);
            case Rotation24Way.ROTATION_12 -> isInside(outerThreshold, innerThreshold, invert - x, invert - y);

            case Rotation24Way.ROTATION_13 -> isInside(outerThreshold, innerThreshold, y, z);
            case Rotation24Way.ROTATION_14 -> isInside(outerThreshold, innerThreshold, y, x);
            case Rotation24Way.ROTATION_15 -> isInside(outerThreshold, innerThreshold, y, invert - z);
            case Rotation24Way.ROTATION_16 -> isInside(outerThreshold, innerThreshold, y, invert - x);

            case Rotation24Way.ROTATION_17 -> isInside(outerThreshold, innerThreshold, z, x);
            case Rotation24Way.ROTATION_18 -> isInside(outerThreshold, innerThreshold, x, invert - z);
            case Rotation24Way.ROTATION_19 -> isInside(outerThreshold, innerThreshold, invert - z, invert - x);
            case Rotation24Way.ROTATION_20 -> isInside(outerThreshold, innerThreshold, z, invert - x);

            case Rotation24Way.ROTATION_21 -> isInside(outerThreshold, innerThreshold, invert - y, z);
            case Rotation24Way.ROTATION_22 -> isInside(outerThreshold, innerThreshold, invert - y, x);
            case Rotation24Way.ROTATION_23 -> isInside(outerThreshold, innerThreshold, invert - y, invert - z);
            case Rotation24Way.ROTATION_24 -> isInside(outerThreshold, innerThreshold, invert - y, invert - x);
        };
    }

    private boolean isInside(int outerThreshold, int innerThreshold, int a, int b) {
        float distance = (a / stepHeight.value()) * slope.value() + b / stepHeight.value();
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 1.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
