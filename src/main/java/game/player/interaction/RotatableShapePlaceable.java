package game.player.interaction;

import core.settings.optionSettings.Option;

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
    protected boolean isBitMapInValid(int settingsHash, int preferredSize) {
        boolean isInvalid = lastrotation != rotation;
        lastrotation = rotation;
        return super.isBitMapInValid(settingsHash, preferredSize) || isInvalid;
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        RotatableShapePlaceable placeable = copyWithMaterialRotatable(material);
        placeable.rotation = rotation;
        return placeable;
    }

    protected abstract RotatableShapePlaceable copyWithMaterialRotatable(byte material);

    protected Option rotation;
    private Option lastrotation;
}
