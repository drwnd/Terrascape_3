package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation6Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;

public final class ConePlaceable extends ShapePlaceable {

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 8);
        saver.saveByte(getMaterial());
        saver.saveInt(baseRadius.value());
        saver.saveInt(topRadius.value());
        saver.saveInt(height.value());
        saver.saveFloat(exponent.value());
        saver.saveInt(thickness.value());
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
        super(ComputeShaders.CONE, material, Rotation6Way.ROTATION_5);
        loadSettings();
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        ConePlaceable copy = new ConePlaceable(material);
        copy.baseRadius.setValue(baseRadius.value());
        copy.topRadius.setValue(topRadius.value());
        copy.height.setValue(height.value());
        copy.exponent.setValue(exponent.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(baseRadius, UiMessages.RADIUS, "baseRadius"),
                new ShapeSetting(topRadius, UiMessages.TOP_RADIUS, "topRadius"),
                new ShapeSetting(exponent, UiMessages.DISTANCE_EXPONENT, "exponent"),
                new ShapeSetting(height, UiMessages.HEIGHT, "height"),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness")
        };
    }

    @Override
    public int getLengthX() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> baseRadius.value() * 2;
            case Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> height.value();
        };
    }

    @Override
    public int getLengthY() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4, Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> baseRadius.value() * 2;
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5 -> height.value();
        };
    }

    @Override
    public int getLengthZ() {
        return switch ((Rotation6Way) rotation()) {
            case Rotation6Way.ROTATION_1, Rotation6Way.ROTATION_4 -> height.value();
            case Rotation6Way.ROTATION_2, Rotation6Way.ROTATION_5, Rotation6Way.ROTATION_3, Rotation6Way.ROTATION_6 -> baseRadius.value() * 2;
        };
    }

    @Override
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting baseRadius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting topRadius = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneIntSetting height = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(1, 128, 128);
}
