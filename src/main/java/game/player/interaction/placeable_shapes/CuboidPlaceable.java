package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation6Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class CuboidPlaceable extends ShapePlaceable {

    public CuboidPlaceable(byte material) {
        super(ComputeShaders.CUBOID, material, Rotation6Way.ROTATION_1);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 13);
        saver.saveByte(getMaterial());
        saver.saveInt(thickness.value());
        saver.saveInt(lengthX.value());
        saver.saveInt(lengthY.value());
        saver.saveInt(lengthZ.value());
    }

    public static CuboidPlaceable load(Saver<?> saver) {
        CuboidPlaceable placeable = new CuboidPlaceable(saver.loadByte());
        placeable.thickness.setValue(saver.loadInt());
        placeable.lengthX.setValue(saver.loadInt());
        placeable.lengthY.setValue(saver.loadInt());
        placeable.lengthZ.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public int unrotatedLengthX() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_2 -> lengthX.value();
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_5 -> lengthY.value();
            case Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_6 -> lengthZ.value();
        };
    }

    @Override
    public int unrotatedLengthY() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_4 -> lengthX.value();
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_6 -> lengthY.value();
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> lengthZ.value();
        };
    }

    @Override
    public int unrotatedLengthZ() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_5, Rotation6Way.ROTATION_6 -> lengthX.value();
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_4 -> lengthY.value();
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_3 -> lengthZ.value();
        };
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CuboidPlaceable copy = new CuboidPlaceable(material);
        copy.thickness.setValue(thickness.value());
        copy.lengthX.setValue(lengthX.value());
        copy.lengthY.setValue(lengthY.value());
        copy.lengthZ.setValue(lengthZ.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(lengthX, UiMessages.LENGTH_X, "radiusA"),
                new ShapeSetting(lengthY, UiMessages.LENGTH_Y, "radiusB"),
                new ShapeSetting(lengthZ, UiMessages.LENGTH_Z, "radiusC")
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting lengthX = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneIntSetting lengthY = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting lengthZ = new StandAloneIntSetting(0, 128, 16);
}
