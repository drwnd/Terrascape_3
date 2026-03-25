package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.ShapePlaceable;
import game.server.MaterialsData;
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(material);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(((CubePlaceable) placeable).getMaterial());
        saver.saveInt(((CubePlaceable) placeable).sizeReduction.value());
        saver.saveInt(((CubePlaceable) placeable).thickness.value());
    }

    public static CubePlaceable load(Saver<?> saver) {
        CubePlaceable placeable = new CubePlaceable(saver.loadByte());
        placeable.sizeReduction.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public int getLengthX() {
        return super.getLengthX() - 2 * sizeReduction.value();
    }

    @Override
    public int getLengthY() {
        return super.getLengthY() - 2 * sizeReduction.value();
    }

    @Override
    public int getLengthZ() {
        return super.getLengthZ() - 2 * sizeReduction.value();
    }

    @Override
    public List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, sizeReduction, UiMessages.SIZE_REDUCTION, true),
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true));
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CubePlaceable copy = new CubePlaceable(material);
        copy.sizeReduction.setValue(sizeReduction.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;
        double innerThreshold = offset - thickness.value();

        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    if (!isInside(x, y, z, offset, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    public Structure getSmallStructure() {
        return new Structure(4, getMaterial());
    }

    private static boolean isInside(int x, int y, int z, double offset, double innerThreshold) {
        double distanceX = Math.abs(x - offset + 0.5);
        double distanceY = Math.abs(y - offset + 0.5);
        double distanceZ = Math.abs(z - offset + 0.5);
        double distance = Math.max(distanceX, Math.max(distanceY, distanceZ));

        return distance >= innerThreshold;
    }

    private final StandAloneIntSetting sizeReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
