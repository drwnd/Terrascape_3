package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.MaterialsData;
import game.server.generation.Structure;

public final class SpherePlaceable extends ShapePlaceable {

    public SpherePlaceable(byte material) {
        super(ComputeShaders.SPHERE, material);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 4);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(thickness.value());
        saver.saveFloat(exponent.value());
    }

    public static SpherePlaceable load(Saver<?> saver) {
        SpherePlaceable placeable = new SpherePlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        placeable.exponent.setValue(saver.loadFloat());
        return placeable;
    }

    @Override
    public ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        SpherePlaceable copy = new SpherePlaceable(material);
        copy.radius.setValue(radius.value());
        copy.thickness.setValue(thickness.value());
        copy.exponent.setValue(exponent.value());
        return copy;
    }

    @Override
    public int getLengthX() {
        return radius.value() * 2;
    }

    @Override
    public int getLengthY() {
        return radius.value() * 2;
    }

    @Override
    public int getLengthZ() {
        return radius.value() * 2;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        double offset = sideLength / 2.0;
        double outerThreshold = Math.pow(radius.value(), exponent.value());
        double innerThreshold = Math.pow(Math.max(0, radius.value() - thickness.value()), exponent.value());

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, offset, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private boolean isInside(int x, int y, int z, double offset, double outerThreshold, double innerThreshold) {
        double distanceX = Math.pow(Math.abs(x - offset + 0.5), exponent.value());
        double distanceY = Math.pow(Math.abs(y - offset + 0.5), exponent.value());
        double distanceZ = Math.pow(Math.abs(z - offset + 0.5), exponent.value());
        double distance = distanceX + distanceY + distanceZ;

        return distance <= outerThreshold && distance >= innerThreshold;
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
