package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.generation.Structure;
import game.settings.IntSettings;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(ComputeShaders.CUBE, material);
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(getMaterial());
        saver.saveInt(sizeReduction.value());
        saver.saveInt(thickness.value());
    }

    public static CubePlaceable load(Saver<?> saver) {
        CubePlaceable placeable = new CubePlaceable(saver.loadByte());
        placeable.sizeReduction.setValue(saver.loadInt());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public int unrotatedLengthX() {
        return (1 << IntSettings.BREAK_PLACE_SIZE.value()) - 2 * sizeReduction.value();
    }

    @Override
    public int unrotatedLengthY() {
        return (1 << IntSettings.BREAK_PLACE_SIZE.value()) - 2 * sizeReduction.value();
    }

    @Override
    public int unrotatedLengthZ() {
        return (1 << IntSettings.BREAK_PLACE_SIZE.value()) - 2 * sizeReduction.value();
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CubePlaceable copy = new CubePlaceable(material);
        copy.sizeReduction.setValue(sizeReduction.value());
        copy.thickness.setValue(thickness.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(sizeReduction, UiMessages.SIZE_REDUCTION, null),
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness")
        };
    }

    @Override
    public Structure getSmallStructure() {
        return new Structure(4, getMaterial());
    }

    private final StandAloneIntSetting sizeReduction = new StandAloneIntSetting(0, 64, 0);
    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 128);
}
