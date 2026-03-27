package game.player.interaction.placeable_shapes;

import core.renderables.UiButton;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation3Way;
import game.player.inventory.CallbackSlider;
import game.server.MaterialsData;
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;

public final class CylinderPlaceable extends RotatableShapePlaceable {

    public CylinderPlaceable(byte material) {
        super(material, Rotation3Way.ROTATION_2);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 5);
        saver.saveByte(((CylinderPlaceable) placeable).getMaterial());
        saver.saveInt(((CylinderPlaceable) placeable).radius.value());
        saver.saveInt(((CylinderPlaceable) placeable).thickness.value());
        saver.saveInt(((CylinderPlaceable) placeable).height.value());
        saver.saveFloat(((CylinderPlaceable) placeable).exponent.value());
    }

    public static CylinderPlaceable load(Saver<?> saver) {
        CylinderPlaceable placeable = new CylinderPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        double offset = (rotation == Rotation3Way.ROTATION_1 ? lengthY : lengthX) / 2.0;
        double outerThreshold = Math.pow(radius.value(), exponent.value());
        double innerThreshold = Math.pow(Math.max(0, radius.value() - thickness.value()), exponent.value());

        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    if (!isInside(x, y, z, offset, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiButton> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new CallbackSlider<>(zero, zero, radius, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true),
                new CallbackSlider<>(zero, zero, height, UiMessages.HEIGHT, true),
                new CallbackSlider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true));
    }

    @Override
    public int getLengthX() {
        return switch (rotation) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_2 -> radius.value() * 2;
            case Rotation3Way.ROTATION_1 -> height.value();
            case null, default -> super.getLengthX();
        };
    }

    @Override
    public int getLengthY() {
        return switch (rotation) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_1 -> radius.value() * 2;
            case Rotation3Way.ROTATION_2 -> height.value();
            case null, default -> super.getLengthY();
        };
    }

    @Override
    public int getLengthZ() {
        return switch (rotation) {
            case Rotation3Way.ROTATION_3 -> height.value();
            case Rotation3Way.ROTATION_2, Rotation3Way.ROTATION_1 -> radius.value() * 2;
            case null, default -> super.getLengthZ();
        };
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        CylinderPlaceable copy = new CylinderPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.thickness.setValue(thickness.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        return copy;
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private boolean isInside(int x, int y, int z, double offset, double outerThreshold, double innerThreshold) {
        return switch (rotation) {
            case Rotation3Way.ROTATION_3 -> isInsideRotated(z, x, y, offset, outerThreshold, innerThreshold);
            case Rotation3Way.ROTATION_2 -> isInsideRotated(y, x, z, offset, outerThreshold, innerThreshold);
            case Rotation3Way.ROTATION_1 -> isInsideRotated(x, y, z, offset, outerThreshold, innerThreshold);

            case null, default -> false;
        };
    }

    private boolean isInsideRotated(int a, int b, int c, double offset, double outerThreshold, double innerThreshold) {
        if (a >= height.value()) return false;
        double distanceB = Math.pow(Math.abs(b - offset + 0.5), exponent.value());
        double distanceC = Math.pow(Math.abs(c - offset + 0.5), exponent.value());
        double distance = distanceC + distanceB;

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
