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
import game.settings.IntSettings;

import org.joml.Vector2f;

import java.util.List;

import static game.utils.Constants.CHUNK_SIZE;

public final class SlopedStairPlaceable extends RotatableShapePlaceable {

    public SlopedStairPlaceable(byte material) {
        super(material, Rotation24Way.BOTTOM_1);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 7);
        saver.saveByte(((SlopedStairPlaceable) placeable).getMaterial());
        saver.saveInt(((SlopedStairPlaceable) placeable).stepHeight.value());
        saver.saveInt(((SlopedStairPlaceable) placeable).heightOffset.value());
        saver.saveFloat(((SlopedStairPlaceable) placeable).slope.value());
    }

    public static SlopedStairPlaceable load(Saver<?> saver) {
        SlopedStairPlaceable placeable = new SlopedStairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.slope.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        SlopedStairPlaceable copy = new SlopedStairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.slope.setValue(slope.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, stepHeight, UiMessages.STEP_HEIGHT, true),
                new Slider<>(zero, zero, heightOffset, UiMessages.HEIGHT, true),
                new Slider<>(zero, zero, slope, UiMessages.SLOPE, true)
        );
    }

    @Override
    protected int getPreferredSize() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    private boolean isInside(int x, int y, int z, int sideLength) {
        int invert = sideLength - 1;
        return switch (rotation) {
            case Rotation24Way.NORTH_1 -> isInside(sideLength, invert - z, y);
            case Rotation24Way.NORTH_2 -> isInside(sideLength, invert - z, x);
            case Rotation24Way.NORTH_3 -> isInside(sideLength, invert - z, invert - y);
            case Rotation24Way.NORTH_4 -> isInside(sideLength, invert - z, invert - x);

            case Rotation24Way.TOP_1 -> isInside(sideLength, invert - y, z);
            case Rotation24Way.TOP_2 -> isInside(sideLength, invert - y, x);
            case Rotation24Way.TOP_3 -> isInside(sideLength, invert - y, invert - z);
            case Rotation24Way.TOP_4 -> isInside(sideLength, invert - y, invert - x);

            case Rotation24Way.WEST_1 -> isInside(sideLength, invert - x, z);
            case Rotation24Way.WEST_2 -> isInside(sideLength, invert - x, y);
            case Rotation24Way.WEST_3 -> isInside(sideLength, invert - x, invert - z);
            case Rotation24Way.WEST_4 -> isInside(sideLength, invert - x, invert - y);

            case Rotation24Way.SOUTH_1 -> isInside(sideLength, z, y);
            case Rotation24Way.SOUTH_2 -> isInside(sideLength, z, x);
            case Rotation24Way.SOUTH_3 -> isInside(sideLength, z, invert - y);
            case Rotation24Way.SOUTH_4 -> isInside(sideLength, z, invert - x);

            case Rotation24Way.BOTTOM_1 -> isInside(sideLength, y, z);
            case Rotation24Way.BOTTOM_2 -> isInside(sideLength, y, x);
            case Rotation24Way.BOTTOM_3 -> isInside(sideLength, y, invert - z);
            case Rotation24Way.BOTTOM_4 -> isInside(sideLength, y, invert - x);

            case Rotation24Way.EAST_1 -> isInside(sideLength, x, z);
            case Rotation24Way.EAST_2 -> isInside(sideLength, x, y);
            case Rotation24Way.EAST_3 -> isInside(sideLength, x, invert - z);
            case Rotation24Way.EAST_4 -> isInside(sideLength, x, invert - y);

            case null, default -> false;
        };
    }

    private boolean isInside(int sideLength, int a, int b) {
        return (a / stepHeight.value()) * slope.value() + b / stepHeight.value() < (sideLength + heightOffset.value()) / stepHeight.value();
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneFloatSetting slope = new StandAloneFloatSetting(1.0F, 16.0F, 2.0F, 0.1F);
}
