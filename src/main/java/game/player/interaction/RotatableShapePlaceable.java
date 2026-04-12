package game.player.interaction;

import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneOptionSetting;

import game.language.UiMessages;

public abstract class RotatableShapePlaceable extends ShapePlaceable {

    protected RotatableShapePlaceable(byte material, Option defaultRotation) {
        super(material);
        rotation = new StandAloneOptionSetting(defaultRotation);
    }

    public void rotateForwards() {
        rotation.setValue(rotation.value().next());
    }

    public void rotateBackwards() {
        rotation.setValue(rotation.value().previous());
    }


    protected Option rotation() {
        return rotation.value();
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        RotatableShapePlaceable placeable = copyWithMaterialRotatable(material);
        placeable.rotation.setValue(rotation.value());
        return placeable;
    }

    @Override
    protected final ShapeSetting[] getSettings() {
        ShapeSetting[] baseSettings = getSettingsRotatable();
        ShapeSetting[] settings = new ShapeSetting[baseSettings.length + 1];
        System.arraycopy(baseSettings, 0, settings, 0, baseSettings.length);
        settings[baseSettings.length] = new ShapeSetting(rotation, UiMessages.ROTATE_SHAPE_BACKWARD, "rotation");
        return settings;
    }

    protected abstract RotatableShapePlaceable copyWithMaterialRotatable(byte material);

    protected abstract ShapeSetting[] getSettingsRotatable();

    private final StandAloneOptionSetting rotation;
}
