package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation6Way;
import game.server.MaterialsData;
import game.settings.IntSettings;

import org.joml.Vector2f;

import java.util.List;

public final class SlabPlaceable extends RotatableShapePlaceable {

    public SlabPlaceable(byte material) {
        super(material, Rotation6Way.BOTTOM);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 13);
        saver.saveByte(((SlabPlaceable) placeable).getMaterial());
        saver.saveInt(((SlabPlaceable) placeable).thickness.value());
    }

    public static SlabPlaceable load(Saver<?> saver) {
        SlabPlaceable placeable = new SlabPlaceable(saver.loadByte());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        SlabPlaceable copy = new SlabPlaceable(material);
        copy.thickness.setValue(thickness.value());
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
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true)
        );
    }

    @Override
    protected int getPreferredSize() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    private boolean isInside(int x, int y, int z, int sideLength) {
        int invert = sideLength - 1;

        return switch (rotation) {

            case Rotation6Way.NORTH -> invert - z < thickness.value();
            case Rotation6Way.TOP -> invert - y < thickness.value();
            case Rotation6Way.WEST -> invert - x < thickness.value();
            case Rotation6Way.SOUTH -> z < thickness.value();
            case Rotation6Way.BOTTOM -> y < thickness.value();
            case Rotation6Way.EAST -> x < thickness.value();

            case null, default -> false;
        };
    }

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 8);
}
