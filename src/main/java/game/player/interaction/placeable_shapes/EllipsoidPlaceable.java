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

public final class EllipsoidPlaceable extends ShapePlaceable {

    public EllipsoidPlaceable(byte material) {
        super(ComputeShaders.ELLIPSOID, material, Rotation6Way.ROTATION_2);
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
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
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
    protected ShapeSetting[] getSettings() {
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

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneIntSetting radiusA = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting radiusB = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting radiusC = new StandAloneIntSetting(0, 128, 16);
    private final StandAloneFloatSetting exponentA = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneFloatSetting exponentB = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
    private final StandAloneFloatSetting exponentC = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
