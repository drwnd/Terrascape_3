package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation12Way;
import game.server.MaterialsData;

import org.joml.Vector2f;

import java.util.List;

import static game.utils.Constants.CHUNK_SIZE;

public final class StairPlaceable extends RotatableShapePlaceable {

    public StairPlaceable(byte material) {
        super(material, Rotation12Way.BOTTOM_NORTH);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 6);
        saver.saveByte(((StairPlaceable) placeable).getMaterial());
        saver.saveInt(((StairPlaceable) placeable).stepHeight.value());
        saver.saveInt(((StairPlaceable) placeable).heightOffset.value());
        saver.saveInt(((StairPlaceable) placeable).thickness.value());
    }

    public static StairPlaceable load(Saver<?> saver) {
        StairPlaceable placeable = new StairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        StairPlaceable copy = new StairPlaceable(material);
        copy.stepHeight.setValue(stepHeight.value());
        copy.heightOffset.setValue(heightOffset.value());
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
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, stepHeight, UiMessages.STEP_HEIGHT, true),
                new Slider<>(zero, zero, heightOffset, UiMessages.HEIGHT, true),
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true)
        );
    }

    private boolean isInside(int x, int y, int z, int sideLength, int outerThreshold, int innerThreshold) {
        int invert = sideLength - 1;
        return switch (rotation) {
            case Rotation12Way.BOTTOM_NORTH -> isInside(outerThreshold, innerThreshold, z, y);
            case Rotation12Way.BOTTOM_WEST -> isInside(outerThreshold, innerThreshold, x, y);
            case Rotation12Way.BOTTOM_SOUTH -> isInside(outerThreshold, innerThreshold, invert - z, y);
            case Rotation12Way.BOTTOM_EAST -> isInside(outerThreshold, innerThreshold, invert - x, y);

            case Rotation12Way.NORTH_WEST -> isInside(outerThreshold, innerThreshold, x, z);
            case Rotation12Way.SOUTH_WEST -> isInside(outerThreshold, innerThreshold, invert - z, x);
            case Rotation12Way.SOUTH_EAST -> isInside(outerThreshold, innerThreshold, invert - x, invert - z);
            case Rotation12Way.NORTH_EAST -> isInside(outerThreshold, innerThreshold, invert - x, z);

            case Rotation12Way.TOP_NORTH -> isInside(outerThreshold, innerThreshold, z, invert - y);
            case Rotation12Way.TOP_WEST -> isInside(outerThreshold, innerThreshold, x, invert - y);
            case Rotation12Way.TOP_SOUTH -> isInside(outerThreshold, innerThreshold, invert - z, invert - y);
            case Rotation12Way.TOP_EAST -> isInside(outerThreshold, innerThreshold, invert - x, invert - y);

            case null, default -> false;
        };
    }

    private boolean isInside(int outerThreshold, int innerThreshold, int a, int b) {
        int distance = a / stepHeight.value() + b / stepHeight.value();
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
