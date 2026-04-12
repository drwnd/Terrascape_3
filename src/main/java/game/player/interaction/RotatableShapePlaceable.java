package game.player.interaction;

import core.settings.optionSettings.Option;
import core.settings.stand_alones.StandAloneOptionSetting;

import game.language.UiMessages;

public abstract class RotatableShapePlaceable extends ShapePlaceable {

    protected RotatableShapePlaceable(byte material, Option defaultRotation) {
        super(material);
        rotation = defaultRotation;
    }

    public void rotateForwards() {
        rotation = rotation.next();
    }

    public void rotateBackwards() {
        rotation = rotation.previous();
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        RotatableShapePlaceable placeable = copyWithMaterialRotatable(material);
        placeable.rotation = rotation;
        return placeable;
    }

    @Override
    protected final ShapeSetting[] getSettings() {
        ShapeSetting[] baseSettings = getSettingsRotatable();
        ShapeSetting[] settings = new ShapeSetting[baseSettings.length + 1];
        System.arraycopy(baseSettings, 0, settings, 0, baseSettings.length);
        settings[baseSettings.length] = new ShapeSetting(new StandAloneOptionSetting(rotation), UiMessages.ROTATE_SHAPE_BACKWARD, "rotation");
        return settings;
    }

    protected abstract RotatableShapePlaceable copyWithMaterialRotatable(byte material);

    protected abstract ShapeSetting[] getSettingsRotatable();

    protected Option rotation;
}
