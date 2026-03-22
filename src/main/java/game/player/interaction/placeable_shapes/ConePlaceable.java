package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.renderables.UiBackgroundElement;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation6Way;
import game.server.MaterialsData;

import org.joml.Vector2f;

import java.util.List;

public final class ConePlaceable extends RotatableShapePlaceable {

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 8);
        saver.saveByte(((ConePlaceable) placeable).getMaterial());
        saver.saveInt(((ConePlaceable) placeable).radiusReduction.value());
        saver.saveInt(((ConePlaceable) placeable).topRadius.value());
        saver.saveInt(((ConePlaceable) placeable).heightOffset.value());
        saver.saveFloat(((ConePlaceable) placeable).exponent.value());
        saver.saveInt(((ConePlaceable) placeable).thickness.value());
    }

    public static ConePlaceable load(Saver<?> saver) {
        ConePlaceable placeable = new ConePlaceable(saver.loadByte());
        placeable.radiusReduction.setValue(saver.loadInt());
        placeable.topRadius.setValue(saver.loadInt());
        placeable.heightOffset.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    public ConePlaceable(byte material) {
        super(material, Rotation6Way.BOTTOM);
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        ConePlaceable copy = new ConePlaceable(material);
        copy.radiusReduction.setValue(radiusReduction.value());
        copy.topRadius.setValue(topRadius.value());
        copy.heightOffset.setValue(heightOffset.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;
        if (offset <= radiusReduction.value()) return;
        double baseRadius = offset - radiusReduction.value();

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength, offset, baseRadius)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, radiusReduction, UiMessages.RADIUS_REDUCTION, true),
                new Slider<>(zero, zero, topRadius, UiMessages.TOP_RADIUS, true),
                new Slider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true),
                new Slider<>(zero, zero, heightOffset, UiMessages.HEIGHT_OFFSET, true),
                new Slider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true)
        );
    }

    private boolean isInside(int x, int y, int z, int sideLength, double offset, double baseRadius) {
        int invert = sideLength - 1;
        return switch (rotation) {
            case Rotation6Way.NORTH -> isInsideRotated(sideLength, offset, baseRadius, invert - z, x, y);
            case Rotation6Way.TOP -> isInsideRotated(sideLength, offset, baseRadius, invert - y, x, z);
            case Rotation6Way.WEST -> isInsideRotated(sideLength, offset, baseRadius, invert - x, y, z);
            case Rotation6Way.SOUTH -> isInsideRotated(sideLength, offset, baseRadius, z, x, y);
            case Rotation6Way.BOTTOM -> isInsideRotated(sideLength, offset, baseRadius, y, x, z);
            case Rotation6Way.EAST -> isInsideRotated(sideLength, offset, baseRadius, x, y, z);
            case null, default -> false;
        };
    }

    private boolean isInsideRotated(int sideLength, double offset, double baseRadius, int a, int b, int c) {
        double heightFraction = (double) a / (sideLength + heightOffset.value());
        if (heightFraction > 1) return false;
        double outerRadius = (1 - heightFraction) * baseRadius + heightFraction * topRadius.value();
        double innerRadius = Math.max(0, outerRadius - thickness.value());

        double outerThreshold = Math.pow(outerRadius, exponent.value());
        double innerThreshold = Math.pow(innerRadius, exponent.value());

        double distanceB = Math.pow(Math.abs(b - offset + 0.5), exponent.value());
        double distanceC = Math.pow(Math.abs(c - offset + 0.5), exponent.value());
        return distanceB + distanceC <= outerThreshold && distanceB + distanceC >= innerThreshold;
    }

    private final StandAloneIntSetting radiusReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting topRadius = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting heightOffset = new StandAloneIntSetting(-32, 32, 0);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(1, 128, 128);
}
