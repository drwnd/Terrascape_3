package game.player.interaction.placeable_shapes;

import core.renderables.UiButton;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Rotation8Way;
import game.player.inventory.CallbackSlider;
import game.server.MaterialsData;
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

public final class InsideArcPlaceable extends RotatableShapePlaceable {

    public InsideArcPlaceable(byte material) {
        super(material, Rotation8Way.ROTATION_1);
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 16);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(exponent.value());
    }

    public static InsideArcPlaceable load(Saver<?> saver) {
        InsideArcPlaceable placeable = new InsideArcPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        InsideArcPlaceable copy = new InsideArcPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected List<UiButton> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new CallbackSlider<>(zero, zero, radius, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true),
                new CallbackSlider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true)
        );
    }

    @Override
    protected int settingsHash() {
        return Objects.hash(radius.value(), thickness.value(), exponent.value());
    }

    @Override
    public int getLengthX() {
        return radius.value();
    }

    @Override
    public int getLengthY() {
        return radius.value();
    }

    @Override
    public int getLengthZ() {
        return radius.value();
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double outerThreshold = Math.pow(radius.value(), exponent.value());
        double innerThreshold = Math.pow(Math.max(0, radius.value() - thickness.value()), exponent.value());

        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        int invertX = lengthX - 1, invertY = lengthY - 1, invertZ = lengthZ - 1;
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    if (!isInside(x, y, z, invertX, invertY, invertZ, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    private boolean isInside(int x, int y, int z, int invertX, int invertY, int invertZ, double outerThreshold, double innerThreshold) {
        return switch ((Rotation8Way) rotation) {
            case ROTATION_1 -> isInside(outerThreshold, innerThreshold, x, y, z);
            case ROTATION_2 -> isInside(outerThreshold, innerThreshold, x, y, invertZ - z);
            case ROTATION_3 -> isInside(outerThreshold, innerThreshold, x, invertY - y, z);
            case ROTATION_4 -> isInside(outerThreshold, innerThreshold, x, invertY - y, invertZ - z);
            case ROTATION_5 -> isInside(outerThreshold, innerThreshold, invertX - x, y, z);
            case ROTATION_6 -> isInside(outerThreshold, innerThreshold, invertX - x, y, invertZ - z);
            case ROTATION_7 -> isInside(outerThreshold, innerThreshold, invertX - x, invertY - y, z);
            case ROTATION_8 -> isInside(outerThreshold, innerThreshold, invertX - x, invertY - y, invertZ - z);
        };
    }

    private boolean isInside(double outerThreshold, double innerThreshold, int x, int y, int z) {
        double distanceX = Math.pow(x + 0.5, exponent.value());
        double distanceY = Math.pow(y + 0.5, exponent.value());
        double distanceZ = Math.pow(z + 0.5, exponent.value());
        double distance = Math.min(distanceX + distanceY, Math.min(distanceX + distanceZ, distanceY + distanceZ));
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.1F, 16.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
