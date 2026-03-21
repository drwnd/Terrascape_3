package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation3Way;
import game.server.MaterialsData;
import org.joml.Vector2f;

import java.util.List;

public final class CylinderPlaceable extends RotatableShapePlaceable {

    public CylinderPlaceable(byte material) {
        super(material, Rotation3Way.Y);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 5);
        saver.saveByte(((CylinderPlaceable) placeable).getMaterial());
        saver.saveInt(((CylinderPlaceable) placeable).radiusReduction.value());
        saver.saveInt(((CylinderPlaceable) placeable).innerRadius.value());
        saver.saveFloat(((CylinderPlaceable) placeable).exponent.value());
    }

    public static CylinderPlaceable load(Saver<?> saver) {
        CylinderPlaceable copy = new CylinderPlaceable(saver.loadByte());
        copy.radiusReduction.setValue(saver.loadInt());
        copy.innerRadius.setValue(saver.loadInt());
        copy.exponent.setValue(saver.loadFloat());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;
        double outerThreshold = Math.pow(offset - radiusReduction.value(), exponent.value());
        double innerThreshold = Math.pow(innerRadius.value(), exponent.value());

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, offset, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, radiusReduction, UiMessages.RADIUS_REDUCTION, true),
                new Slider<>(zero, zero, innerRadius, UiMessages.INNER_RADIUS, true),
                new Slider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true));
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        CylinderPlaceable placeable = new CylinderPlaceable(material);
        placeable.radiusReduction.setValue(radiusReduction.value());
        placeable.innerRadius.setValue(innerRadius.value());
        placeable.exponent.setValue(exponent.value());
        return placeable;
    }

    private boolean isInside(int x, int y, int z, double offset, double outerThreshold, double innerThreshold) {
        if (rotation == Rotation3Way.X) return isInside(y, z, offset, outerThreshold, innerThreshold);
        if (rotation == Rotation3Way.Y) return isInside(x, z, offset, outerThreshold, innerThreshold);
        return isInside(x, y, offset, outerThreshold, innerThreshold);
    }

    private boolean isInside(int a, int b, double offset, double outerThreshold, double innerThreshold) {
        double distanceA = Math.pow(Math.abs(a - offset + 0.5), exponent.value());
        double distanceB = Math.pow(Math.abs(b - offset + 0.5), exponent.value());
        double distance = distanceA + distanceB;

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radiusReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting innerRadius = new StandAloneIntSetting(0, 128, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
