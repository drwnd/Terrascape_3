package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation3Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class CylinderPlaceable extends ShapePlaceable {

    public CylinderPlaceable(byte material) {
        super(ComputeShaders.CYLINDER, material, Rotation3Way.ROTATION_2);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 5);
        saver.saveByte(getMaterial());
        saver.saveInt(radius.value());
        saver.saveInt(thickness.value());
        saver.saveInt(height.value());
        saver.saveFloat(exponent.value());
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
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, "radius"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness"),
                new ShapeSetting(height, UiMessages.HEIGHT, "height"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent")
        };
    }

    @Override
    public int getLengthX() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_2 -> radius.value() * 2;
            case Rotation3Way.ROTATION_1 -> height.value();
        };
    }

    @Override
    public int getLengthY() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3, Rotation3Way.ROTATION_1 -> radius.value() * 2;
            case Rotation3Way.ROTATION_2 -> height.value();
        };
    }

    @Override
    public int getLengthZ() {
        return switch ((Rotation3Way) rotation()) {
            case Rotation3Way.ROTATION_3 -> height.value();
            case Rotation3Way.ROTATION_2, Rotation3Way.ROTATION_1 -> radius.value() * 2;
        };
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
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

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
