package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation3Way;
import game.server.MaterialsData;
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;

public final class SlabPlaceable extends RotatableShapePlaceable {

    public SlabPlaceable(byte material) {
        super(material, Rotation3Way.Y);
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
    public int getLengthX() {
        return rotation == Rotation3Way.X ? thickness.value() : super.getLengthX();
    }

    @Override
    public int getLengthY() {
        return rotation == Rotation3Way.Y ? thickness.value() : super.getLengthY();
    }

    @Override
    public int getLengthZ() {
        return rotation == Rotation3Way.Z ? thickness.value() : super.getLengthZ();
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        SlabPlaceable copy = new SlabPlaceable(material);
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true)
        );
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        int lengthX = rotation == Rotation3Way.X ? thickness.value() : 16;
        int lengthY = rotation == Rotation3Way.Y ? thickness.value() : 16;
        int lengthZ = rotation == Rotation3Way.Z ? thickness.value() : 16;
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
        return new Structure(4, getMaterial(), bitMap);
    }

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 8);
}
