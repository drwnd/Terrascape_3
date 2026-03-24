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
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;

public final class CylinderPlaceable extends RotatableShapePlaceable {

    public CylinderPlaceable(byte material) {
        super(material, Rotation6Way.BOTTOM);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 5);
        saver.saveByte(((CylinderPlaceable) placeable).getMaterial());
        saver.saveInt(((CylinderPlaceable) placeable).radius.value());
        saver.saveInt(((CylinderPlaceable) placeable).innerRadius.value());
        saver.saveInt(((CylinderPlaceable) placeable).height.value());
        saver.saveFloat(((CylinderPlaceable) placeable).exponent.value());
    }

    public static CylinderPlaceable load(Saver<?> saver) {
        CylinderPlaceable placeable = new CylinderPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.innerRadius.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;
        double outerThreshold = Math.pow(radius.value(), exponent.value());
        double innerThreshold = Math.pow(innerRadius.value(), exponent.value());

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength, offset, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiBackgroundElement> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, radius, UiMessages.RADIUS, true),
                new Slider<>(zero, zero, innerRadius, UiMessages.INNER_RADIUS, true),
                new Slider<>(zero, zero, height, UiMessages.HEIGHT, true),
                new Slider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true));
    }

    @Override
    public int getLengthX() {
        return switch (rotation) {
            case Rotation6Way.NORTH, Rotation6Way.SOUTH, Rotation6Way.TOP, Rotation6Way.BOTTOM -> radius.value() * 2;
            case Rotation6Way.WEST, Rotation6Way.EAST -> height.value();
            case null, default -> super.getLengthX();
        };
    }

    @Override
    public int getLengthY() {
        return switch (rotation) {
            case Rotation6Way.NORTH, Rotation6Way.SOUTH, Rotation6Way.WEST, Rotation6Way.EAST -> radius.value() * 2;
            case Rotation6Way.TOP, Rotation6Way.BOTTOM -> height.value();
            case null, default -> super.getLengthY();
        };
    }

    @Override
    public int getLengthZ() {
        return switch (rotation) {
            case Rotation6Way.NORTH, Rotation6Way.SOUTH -> height.value();
            case Rotation6Way.TOP, Rotation6Way.BOTTOM, Rotation6Way.WEST, Rotation6Way.EAST -> radius.value() * 2;
            case null, default -> super.getLengthZ();
        };
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        CylinderPlaceable copy = new CylinderPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.innerRadius.setValue(innerRadius.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        return copy;
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private boolean isInside(int x, int y, int z, int sideLength, double offset, double outerThreshold, double innerThreshold) {
        int invert = sideLength - 1;

        return switch (rotation) {
            case Rotation6Way.NORTH -> isInside(invert - z, x, y, offset, outerThreshold, innerThreshold);
            case Rotation6Way.TOP -> isInside(invert - y, x, z, offset, outerThreshold, innerThreshold);
            case Rotation6Way.WEST -> isInside(invert - x, y, z, offset, outerThreshold, innerThreshold);
            case Rotation6Way.SOUTH -> isInside(z, x, y, offset, outerThreshold, innerThreshold);
            case Rotation6Way.BOTTOM -> isInside(y, x, z, offset, outerThreshold, innerThreshold);
            case Rotation6Way.EAST -> isInside(x, y, z, offset, outerThreshold, innerThreshold);

            case null, default -> false;
        };
    }

    private boolean isInside(int a, int b, int c, double offset, double outerThreshold, double innerThreshold) {
        if (a >= height.value()) return false;
        double distanceB = Math.pow(Math.abs(b - offset + 0.5), exponent.value());
        double distanceC = Math.pow(Math.abs(c - offset + 0.5), exponent.value());
        double distance = distanceC + distanceB;

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting innerRadius = new StandAloneIntSetting(0, 128, 0);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
