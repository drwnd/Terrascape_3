package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.ShapePlaceable;
import game.server.MaterialsData;
import org.joml.Vector2f;

import java.util.List;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(material);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(((CubePlaceable) placeable).getMaterial());
        saver.saveInt(((CubePlaceable) placeable).radiusReduction.value());
        saver.saveInt(((CubePlaceable) placeable).innerRadius.value());
    }

    public static CubePlaceable load(Saver<?> saver) {
        CubePlaceable placeable = new CubePlaceable(saver.loadByte());
        placeable.radiusReduction.setValue(saver.loadInt());
        placeable.innerRadius.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, radiusReduction, UiMessages.RADIUS_REDUCTION, true),
                new Slider<>(zero, zero, innerRadius, UiMessages.INNER_RADIUS, true));
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CubePlaceable copy = new CubePlaceable(material);
        copy.radiusReduction.setValue(radiusReduction.value());
        copy.innerRadius.setValue(innerRadius.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int offset = sideLength / 2;
        int outerThreshold = offset - radiusReduction.value();
        int innerThreshold = innerRadius.value();

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, offset, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    private static boolean isInside(int x, int y, int z, int offset, int outerThreshold, int innerThreshold) {
        double distanceX = Math.abs(x - offset + 0.5);
        double distanceY = Math.abs(y - offset + 0.5);
        double distanceZ = Math.abs(z - offset + 0.5);
        double distance = Math.max(distanceX, Math.max(distanceY, distanceZ));

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radiusReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting innerRadius = new StandAloneIntSetting(0, 128, 0);
}
