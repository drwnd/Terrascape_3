package game.player.interaction.placeable_shapes;

import core.renderables.UiButton;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation6Way;
import game.player.inventory.CallbackSlider;
import game.server.MaterialsData;
import game.server.generation.Structure;

import org.joml.Vector2f;

import java.util.List;

public final class ConePlaceable extends RotatableShapePlaceable {

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 8);
        saver.saveByte(((ConePlaceable) placeable).getMaterial());
        saver.saveInt(((ConePlaceable) placeable).baseRadius.value());
        saver.saveInt(((ConePlaceable) placeable).topRadius.value());
        saver.saveInt(((ConePlaceable) placeable).height.value());
        saver.saveFloat(((ConePlaceable) placeable).exponent.value());
        saver.saveInt(((ConePlaceable) placeable).thickness.value());
    }

    public static ConePlaceable load(Saver<?> saver) {
        ConePlaceable placeable = new ConePlaceable(saver.loadByte());
        placeable.baseRadius.setValue(saver.loadInt());
        placeable.topRadius.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    public ConePlaceable(byte material) {
        super(material, Rotation6Way.ROTATION_5);
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        ConePlaceable copy = new ConePlaceable(material);
        copy.baseRadius.setValue(baseRadius.value());
        copy.topRadius.setValue(topRadius.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;

        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    if (!isInside(x, y, z, lengthX, lengthY, lengthZ, offset)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    protected List<UiButton> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new CallbackSlider<>(zero, zero, baseRadius, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, topRadius, UiMessages.TOP_RADIUS, true),
                new CallbackSlider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT, true),
                new CallbackSlider<>(zero, zero, height, UiMessages.HEIGHT, true),
                new CallbackSlider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true)
        );
    }

    @Override
    public int getLengthX() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> baseRadius.value() * 2;
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> height.value();
            case null, default -> super.getLengthX();
        };
    }

    @Override
    public int getLengthY() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> baseRadius.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> height.value();
            case null, default -> super.getLengthY();
        };
    }

    @Override
    public int getLengthZ() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4 -> height.value();
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5, Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> baseRadius.value() * 2;
            case null, default -> super.getLengthZ();
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private boolean isInside(int x, int y, int z, int lengthX, int lengthY, int lengthZ, double offset) {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1 -> isInside(offset, lengthZ - 1 - z, x, y);
            case Rotation6Way.ROTATION_2 -> isInside(offset, lengthY - 1 - y, x, z);
            case Rotation6Way.ROTATION_3 -> isInside(offset, lengthX  - 1- x, y, z);
            case Rotation6Way.ROTATION_4 -> isInside(offset, z, x, y);
            case Rotation6Way.ROTATION_5 -> isInside(offset, y, x, z);
            case Rotation6Way.ROTATION_6 -> isInside(offset, x, y, z);
            case null, default -> false;
        };
    }

    private boolean isInside(double offset, int a, int b, int c) {
        double heightFraction = (double) a / height.value();
        if (heightFraction > 1) return false;
        double outerRadius = (1 - heightFraction) * baseRadius.value() + heightFraction * topRadius.value();
        double innerRadius = Math.max(0, outerRadius - thickness.value());

        double outerThreshold = Math.pow(outerRadius, exponent.value());
        double innerThreshold = Math.pow(innerRadius, exponent.value());

        double distanceB = Math.pow(Math.abs(b - offset + 0.5), exponent.value());
        double distanceC = Math.pow(Math.abs(c - offset + 0.5), exponent.value());
        return distanceB + distanceC <= outerThreshold && distanceB + distanceC >= innerThreshold;
    }

    private final StandAloneIntSetting baseRadius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting topRadius = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(1, 128, 128);
}
