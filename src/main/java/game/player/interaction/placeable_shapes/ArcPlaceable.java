package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation12Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.MaterialsData;
import game.server.generation.Structure;

public final class ArcPlaceable extends ShapePlaceable {

    public ArcPlaceable(byte material) {
        super(ComputeShaders.ARC, material, Rotation12Way.ROTATION_01);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 15);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(height.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(exponent.value());
    }

    public static ArcPlaceable load(Saver<?> saver) {
        ArcPlaceable placeable = new ArcPlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.height.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        ArcPlaceable copy = new ArcPlaceable(material);
        copy.radius.setValue(radius.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(height, UiMessages.HEIGHT, "height"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    @Override
    public int getLengthX() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_01, ROTATION_03, ROTATION_09, ROTATION_11 -> height.value();
            case ROTATION_02, ROTATION_04, ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08, ROTATION_10, ROTATION_12 -> radius.value();
        };
    }

    @Override
    public int getLengthY() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08 -> height.value();
            case ROTATION_01, ROTATION_02, ROTATION_03, ROTATION_04, ROTATION_09, ROTATION_10, ROTATION_11, ROTATION_12 -> radius.value();
        };
    }

    @Override
    public int getLengthZ() {
        return switch ((Rotation12Way) rotation()) {
            case ROTATION_02, ROTATION_04, ROTATION_10, ROTATION_12 -> height.value();
            case ROTATION_01, ROTATION_03, ROTATION_05, ROTATION_06, ROTATION_07, ROTATION_08, ROTATION_09, ROTATION_11 -> radius.value();
        };
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
        return switch ((Rotation12Way) rotation()) {
            case Rotation12Way.ROTATION_01 -> isInside(outerThreshold, innerThreshold, z, y);
            case Rotation12Way.ROTATION_02 -> isInside(outerThreshold, innerThreshold, x, y);
            case Rotation12Way.ROTATION_03 -> isInside(outerThreshold, innerThreshold, invertZ - z, y);
            case Rotation12Way.ROTATION_04 -> isInside(outerThreshold, innerThreshold, invertX - x, y);

            case Rotation12Way.ROTATION_05 -> isInside(outerThreshold, innerThreshold, x, z);
            case Rotation12Way.ROTATION_06 -> isInside(outerThreshold, innerThreshold, invertZ - z, x);
            case Rotation12Way.ROTATION_07 -> isInside(outerThreshold, innerThreshold, invertX - x, invertZ - z);
            case Rotation12Way.ROTATION_08 -> isInside(outerThreshold, innerThreshold, invertX - x, z);

            case Rotation12Way.ROTATION_09 -> isInside(outerThreshold, innerThreshold, z, invertY - y);
            case Rotation12Way.ROTATION_10 -> isInside(outerThreshold, innerThreshold, x, invertY - y);
            case Rotation12Way.ROTATION_11 -> isInside(outerThreshold, innerThreshold, invertZ - z, invertY - y);
            case Rotation12Way.ROTATION_12 -> isInside(outerThreshold, innerThreshold, invertX - x, invertY - y);
        };
    }

    private boolean isInside(double outerThreshold, double innerThreshold, int a, int b) {
        double distanceA = Math.pow(a + 0.5, exponent.value());
        double distanceB = Math.pow(b + 0.5, exponent.value());
        double distance = distanceA + distanceB;
        return distance < outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 8);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.1F, 16.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
