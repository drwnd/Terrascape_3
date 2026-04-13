package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
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
    public Structure getSmallStructure() {
        return getStructure();
    }

    private final StandAloneIntSetting radius = new StandAloneIntSetting(0, 128, 8);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
