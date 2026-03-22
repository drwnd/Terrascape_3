package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation24Way;
import game.server.MaterialsData;
import org.joml.Vector2f;

import java.util.List;

import static game.utils.Constants.CHUNK_SIZE;

public final class OutsideSlopedStairPlaceable extends RotatableShapePlaceable {

    public OutsideSlopedStairPlaceable(byte material) {
        super(material, Rotation24Way.BOTTOM_1);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 12);
        saver.saveByte(((OutsideSlopedStairPlaceable) placeable).getMaterial());
        saver.saveInt(((OutsideSlopedStairPlaceable) placeable).stepHeight.value());
        saver.saveInt(((OutsideSlopedStairPlaceable) placeable).heightOffset.value());
        saver.saveFloat(((OutsideSlopedStairPlaceable) placeable).slope.value());
    }

    public static OutsideSlopedStairPlaceable load(Saver<?> saver) {
        OutsideSlopedStairPlaceable placeable = new OutsideSlopedStairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        OutsideSlopedStairPlaceable copy = new OutsideSlopedStairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.slope.setValue(slope.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int threshold = (sideLength + heightOffset.value()) / stepHeight.value();

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength, threshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, stepHeight, UiMessages.STEP_HEIGHT, true),
                new Slider<>(zero, zero, heightOffset, UiMessages.HEIGHT_OFFSET, true),
                new Slider<>(zero, zero, slope, UiMessages.SLOPE, true)
        );
    }

    private boolean isInside(int x, int y, int z, int sideLength, int threshold) {
        int invert = sideLength - 1;
        return switch (rotation) {
            case Rotation24Way.NORTH_1 -> isInside(threshold, invert - z, x, y);
            case Rotation24Way.NORTH_2 -> isInside(threshold, invert - z, x, invert - y);
            case Rotation24Way.NORTH_3 -> isInside(threshold, invert - z, invert - x, invert - y);
            case Rotation24Way.NORTH_4 -> isInside(threshold, invert - z, invert - x, y);

            case Rotation24Way.TOP_1 -> isInside(threshold, invert - y, x, z);
            case Rotation24Way.TOP_2 -> isInside(threshold, invert - y, x, invert - z);
            case Rotation24Way.TOP_3 -> isInside(threshold, invert - y, invert - x, invert - z);
            case Rotation24Way.TOP_4 -> isInside(threshold, invert - y, invert - x, z);

            case Rotation24Way.WEST_1 -> isInside(threshold, invert - x, y, z);
            case Rotation24Way.WEST_2 -> isInside(threshold, invert - x, y, invert - z);
            case Rotation24Way.WEST_3 -> isInside(threshold, invert - x, invert - y, invert - z);
            case Rotation24Way.WEST_4 -> isInside(threshold, invert - x, invert - y, z);

            case Rotation24Way.SOUTH_1 -> isInside(threshold, z, x, y);
            case Rotation24Way.SOUTH_2 -> isInside(threshold, z, x, invert - y);
            case Rotation24Way.SOUTH_3 -> isInside(threshold, z, invert - x, invert - y);
            case Rotation24Way.SOUTH_4 -> isInside(threshold, z, invert - x, y);

            case Rotation24Way.BOTTOM_1 -> isInside(threshold, y, x, z);
            case Rotation24Way.BOTTOM_2 -> isInside(threshold, y, x, invert - z);
            case Rotation24Way.BOTTOM_3 -> isInside(threshold, y, invert - x, invert - z);
            case Rotation24Way.BOTTOM_4 -> isInside(threshold, y, invert - x, z);

            case Rotation24Way.EAST_1 -> isInside(threshold, x, y, z);
            case Rotation24Way.EAST_2 -> isInside(threshold, x, y, invert - z);
            case Rotation24Way.EAST_3 -> isInside(threshold, x, invert - y, invert - z);
            case Rotation24Way.EAST_4 -> isInside(threshold, x, invert - y, z);

            case null, default -> false;
        };
    }

    private boolean isInside(int threshold, int a, int b, int c) {
        return a / stepHeight.value() * slope.value() + Math.max(b, c) / stepHeight.value() < threshold;
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 2.0F, 0.1F);
}
