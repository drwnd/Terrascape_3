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
import java.util.Objects;

public final class EllipsoidPlaceable extends RotatableShapePlaceable {

    public EllipsoidPlaceable(byte material) {
        super(material, Rotation6Way.ROTATION_2);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 14);
        saver.saveByte(((EllipsoidPlaceable) placeable).getMaterial());
        saver.saveInt(((EllipsoidPlaceable) placeable).thickness.value());
        saver.saveInt(((EllipsoidPlaceable) placeable).radiusA.value());
        saver.saveInt(((EllipsoidPlaceable) placeable).radiusB.value());
        saver.saveInt(((EllipsoidPlaceable) placeable).radiusC.value());
        saver.saveFloat(((EllipsoidPlaceable) placeable).exponentA.value());
        saver.saveFloat(((EllipsoidPlaceable) placeable).exponentB.value());
        saver.saveFloat(((EllipsoidPlaceable) placeable).exponentC.value());
    }

    public static EllipsoidPlaceable load(Saver<?> saver) {
        EllipsoidPlaceable placeable = new EllipsoidPlaceable(saver.loadByte());
        placeable.thickness.setValue(saver.loadInt());
        placeable.radiusA.setValue(saver.loadInt());
        placeable.radiusB.setValue(saver.loadInt());
        placeable.radiusC.setValue(saver.loadInt());
        placeable.exponentA.setValue(saver.loadFloat());
        placeable.exponentB.setValue(saver.loadFloat());
        placeable.exponentC.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected RotatableShapePlaceable copyWithMaterialRotatable(byte material) {
        EllipsoidPlaceable copy = new EllipsoidPlaceable(material);
        copy.thickness.setValue(thickness.value());
        copy.radiusA.setValue(radiusA.value());
        copy.radiusB.setValue(radiusB.value());
        copy.radiusC.setValue(radiusC.value());
        copy.exponentA.setValue(exponentA.value());
        copy.exponentB.setValue(exponentB.value());
        copy.exponentC.setValue(exponentC.value());
        return copy;
    }

    @Override
    public int getLengthX() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_2 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_5 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_6 -> radiusC.value() * 2;
            case null, default -> super.getLengthX();
        };
    }

    @Override
    public int getLengthY() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_4 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_6 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> radiusC.value() * 2;
            case null, default -> super.getLengthY();
        };
    }

    @Override
    public int getLengthZ() {
        return switch (rotation) {
            case Rotation6Way.ROTATION_5, Rotation6Way.ROTATION_6 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_4 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_3 -> radiusC.value() * 2;
            case null, default -> super.getLengthX();
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    @Override
    protected List<UiButton> uniqueSettings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new CallbackSlider<>(zero, zero, thickness, UiMessages.WALL_THICKNESS, true),
                new CallbackSlider<>(zero, zero, radiusA, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, radiusB, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, radiusC, UiMessages.RADIUS, true),
                new CallbackSlider<>(zero, zero, exponentA, UiMessages.DISTANCE_EXPONENT, true),
                new CallbackSlider<>(zero, zero, exponentB, UiMessages.DISTANCE_EXPONENT, true),
                new CallbackSlider<>(zero, zero, exponentC, UiMessages.DISTANCE_EXPONENT, true)
        );
    }

    @Override
    protected int settingsHash() {
        return Objects.hash(thickness.value(), radiusA.value(), radiusB.value(), radiusC.value(), exponentA.value(), exponentB.value(), exponentC.value());
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();

        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    if (!isInside(
                            (x + 0.5 - lengthX * 0.5),
                            (y + 0.5 - lengthY * 0.5),
                            (z + 0.5 - lengthZ * 0.5)
                    )) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    private boolean isInside(double x, double y, double z) {
        return switch (rotation) {
            case Rotation6Way.ROTATION_1 -> isInsideRotated(x, y, z);
            case Rotation6Way.ROTATION_2 -> isInsideRotated(x, z, y);
            case Rotation6Way.ROTATION_3 -> isInsideRotated(y, x, z);
            case Rotation6Way.ROTATION_4 -> isInsideRotated(y, z, x);
            case Rotation6Way.ROTATION_5 -> isInsideRotated(z, x, y);
            case Rotation6Way.ROTATION_6 -> isInsideRotated(z, y, x);
            case null, default -> false;
        };
    }

    private boolean isInsideRotated(double a, double b, double c) {
        double distanceA, distanceB, distanceC;

        distanceA = Math.pow(Math.abs(a / radiusA.value()), exponentA.value());
        distanceB = Math.pow(Math.abs(b / radiusB.value()), exponentB.value());
        distanceC = Math.pow(Math.abs(c / radiusC.value()), exponentC.value());
        double outerDistance = distanceA + distanceB + distanceC;

        distanceA = Math.pow(Math.abs(a / Math.max(0, radiusA.value() - thickness.value())), exponentA.value());
        distanceB = Math.pow(Math.abs(b / Math.max(0, radiusB.value() - thickness.value())), exponentB.value());
        distanceC = Math.pow(Math.abs(c / Math.max(0, radiusC.value() - thickness.value())), exponentC.value());
        double innerDistance = distanceA + distanceB + distanceC;

        return outerDistance <= 1.0 && innerDistance >= 1;
    }

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting radiusA = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting radiusB = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting radiusC = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneFloatSetting exponentA = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneFloatSetting exponentB = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneFloatSetting exponentC = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
