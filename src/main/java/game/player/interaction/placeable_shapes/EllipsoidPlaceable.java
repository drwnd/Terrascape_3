package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.RotatableShapePlaceable;
import game.player.interaction.Rotation6Way;
import game.player.interaction.ShapeSetting;
import game.server.MaterialsData;
import game.server.generation.Structure;

public final class EllipsoidPlaceable extends RotatableShapePlaceable {

    public EllipsoidPlaceable(byte material) {
        super(material, Rotation6Way.ROTATION_2);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 14);
        saver.saveByte(getMaterial());
        saver.saveInt(thickness.value());
        saver.saveInt(radiusA.value());
        saver.saveInt(radiusB.value());
        saver.saveInt(radiusC.value());
        saver.saveFloat(exponentA.value());
        saver.saveFloat(exponentB.value());
        saver.saveFloat(exponentC.value());
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
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_2 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_5 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_6 -> radiusC.value() * 2;
        };
    }

    @Override
    public int getLengthY() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_4 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_6 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> radiusC.value() * 2;
        };
    }

    @Override
    public int getLengthZ() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_5, Rotation6Way.ROTATION_6 -> radiusA.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_4 -> radiusB.value() * 2;
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_3 -> radiusC.value() * 2;
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    @Override
    protected ShapeSetting[] getSettingsRotatable() {
        return new ShapeSetting[]{
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(radiusA, UiMessages.RADIUS, "radiusA"),
                new ShapeSetting(radiusB, UiMessages.RADIUS, "radiusB"),
                new ShapeSetting(radiusC, UiMessages.RADIUS, "radiusC"),
                new ShapeSetting(exponentA, UiMessages.DISTANCE_EXPONENT, "exponentA"),
                new ShapeSetting(exponentB, UiMessages.DISTANCE_EXPONENT, "exponentB"),
                new ShapeSetting(exponentC, UiMessages.DISTANCE_EXPONENT, "exponentC")
        };
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int lengthX = getLengthX(), lengthY = getLengthY(), lengthZ = getLengthZ();

        invOuterRadiusA = 1.0 / radiusA.value();
        invOuterRadiusB = 1.0 / radiusB.value();
        invOuterRadiusC = 1.0 / radiusC.value();
        invInnerRadiusA = 1.0 / Math.max(0, radiusA.value() - thickness.value());
        invInnerRadiusB = 1.0 / Math.max(0, radiusB.value() - thickness.value());
        invInnerRadiusC = 1.0 / Math.max(0, radiusC.value() - thickness.value());

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
        return switch (rotation()) {
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

        distanceA = Math.pow(Math.abs(a * invOuterRadiusA), exponentA.value());
        distanceB = Math.pow(Math.abs(b * invOuterRadiusB), exponentB.value());
        distanceC = Math.pow(Math.abs(c * invOuterRadiusC), exponentC.value());
        double outerDistance = distanceA + distanceB + distanceC;

        distanceA = Math.pow(Math.abs(a * invInnerRadiusA), exponentA.value());
        distanceB = Math.pow(Math.abs(b * invInnerRadiusB), exponentB.value());
        distanceC = Math.pow(Math.abs(c * invInnerRadiusC), exponentC.value());
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

    private double invOuterRadiusA, invOuterRadiusB, invOuterRadiusC;
    private double invInnerRadiusA, invInnerRadiusB, invInnerRadiusC;
}
