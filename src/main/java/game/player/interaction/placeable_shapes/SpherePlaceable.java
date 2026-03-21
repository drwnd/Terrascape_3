package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.ShapePlaceable;
import game.server.MaterialsData;

import org.joml.Vector2f;

import java.util.List;

public final class SpherePlaceable extends ShapePlaceable {

    public SpherePlaceable(byte material) {
        super(material);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 4);
        saver.saveByte(((SpherePlaceable) placeable).getMaterial());
        saver.saveInt(((SpherePlaceable) placeable).radiusReduction.value());
        saver.saveInt(((SpherePlaceable) placeable).innerRadius.value());
        saver.saveFloat(((SpherePlaceable) placeable).exponent.value());
    }

    public static SpherePlaceable load(Saver<?> saver) {
        SpherePlaceable placeable = new SpherePlaceable(saver.loadByte());
        placeable.radiusReduction.setValue(saver.loadInt());
        placeable.innerRadius.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    public List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, radiusReduction, UiMessages.RADIUS_REDUCTION, true),
                new Slider<>(zero, zero, innerRadius, UiMessages.INNER_RADIUS, true),
                new Slider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true));
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        SpherePlaceable copy = new SpherePlaceable(material);
        copy.radiusReduction.setValue(radiusReduction.value());
        copy.innerRadius.setValue(innerRadius.value());
        copy.exponent.setValue(exponent.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int offset = sideLength / 2;
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

    private boolean isInside(int x, int y, int z, int offset, double outerThreshold, double innerThreshold) {
        double distanceX = Math.pow(Math.abs(x - offset + 0.5), exponent.value());
        double distanceY = Math.pow(Math.abs(y - offset + 0.5), exponent.value());
        double distanceZ = Math.pow(Math.abs(z - offset + 0.5), exponent.value());
        double distance = distanceX + distanceY + distanceZ;

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radiusReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting innerRadius = new StandAloneIntSetting(0, 128, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
