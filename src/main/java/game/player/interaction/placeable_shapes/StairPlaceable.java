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
    }

    public static StairPlaceable load(Saver<?> saver) {
        StairPlaceable placeable = new StairPlaceable(saver.loadByte());
        placeable.stepHeight.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        StairPlaceable placeable = new StairPlaceable(material);
        placeable.stepHeight.setValue(stepHeight.value());
        return placeable;
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
        Vector2f zero = new Vector2f(0.0F, 0.0F);
        return List.of(
                new Slider<>(zero, zero, stepHeight, UiMessages.STEP_HEIGHT, true)
        );
    }

    private boolean isInside(int x, int y, int z, int sideLength) {
        int invert = sideLength - 1;
        return switch (rotation) {
            case Rotation12Way.BOTTOM_NORTH -> isInside(sideLength, z, y);
            case Rotation12Way.BOTTOM_WEST -> isInside(sideLength, x, y);
            case Rotation12Way.BOTTOM_SOUTH -> isInside(sideLength, invert - z, y);
            case Rotation12Way.BOTTOM_EAST -> isInside(sideLength, invert - x, y);

            case Rotation12Way.NORTH_WEST -> isInside(sideLength, x, z);
            case Rotation12Way.SOUTH_WEST -> isInside(sideLength, invert - z, x);
            case Rotation12Way.SOUTH_EAST -> isInside(sideLength, invert - x, invert - z);
            case Rotation12Way.NORTH_EAST -> isInside(sideLength, invert - x, z);

            case Rotation12Way.TOP_NORTH -> isInside(sideLength, z, invert - y);
            case Rotation12Way.TOP_WEST -> isInside(sideLength, x, invert - y);
            case Rotation12Way.TOP_SOUTH -> isInside(sideLength, invert - z, invert - y);
            case Rotation12Way.TOP_EAST -> isInside(sideLength, invert - x, invert - y);

            case null, default -> false;
        };
    }

    private boolean isInside(int sideLength, int a, int b) {
        return a / stepHeight.value() + b / stepHeight.value() < sideLength / stepHeight.value();
    }

    private final StandAloneIntSetting stepHeight = new StandAloneIntSetting(1, CHUNK_SIZE / 2, 4);
}
