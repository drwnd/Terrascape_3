package game.player.inventory;

import core.renderables.*;
import core.rendering_api.Window;

import game.player.interaction.ShapePlaceable;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class ShapeDisplay extends UiButton {

    public ShapeDisplay(int index, ShapePlaceable placeable, ShapesTab shapesTab) {
        super(getSizeToParent(shapesTab), getOffsetToParent(index, shapesTab));

        this.placeable = placeable;
        this.index = index;

        setAction(getAction());
        setRimThicknessMultiplier(0.5F);
        setDoAutoFocusScaling(false);
        setScalingFactor(1.2F);

        index = 0;
        for (UiButton settingElement : placeable.getSettingButtons()) {
            if (settingElement instanceof CallbackSlider<?> slider) slider.setSlidingCallback(shapesTab::refreshShapePreview);
            if (settingElement instanceof UiButton settingButton) {
                Clickable clickable = settingButton.getClickable();
                settingButton.setAction((Vector2i pixelCoordinate, int button, int action) -> {
                    ButtonResult result = clickable.clickOn(pixelCoordinate, button, action);
                    if (result != ButtonResult.SUCCESS) return result;
                    if (action != GLFW_PRESS) return ButtonResult.IGNORE;
                    shapesTab.refreshShapePreview();
                    return ButtonResult.SUCCESS;
                });
            }
            settingElement.setSizeToParent(0.3F, 0.075F);
            settingElement.setOffsetToParent(0.35F, 1.0F - 0.05F * Window.getAspectRatio() - index++ * 0.08F);
            settingElement.setVisible(false);
            settingElement.setRimThicknessMultiplier(0.5F);
            settingElements.add(settingElement);
        }

        shapesTab.getShapePlaceableSettingSliders().addAll(settingElements);
        for (UiBackgroundElement settingElement : settingElements) shapesTab.addRenderable(settingElement);

        addRenderable(new StructureDisplay(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), placeable.updateBitMap(false).getSmallStructure()));
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        if (((ShapesTab) getParent()).getSelectedDisplay() == this) scaleForFocused(position, size);
        super.renderSelf(position, size);
    }

    @Override
    public void resizeSelfTo(int width, int height) {
        Vector2f sizeToParent = getSizeToParent(getParent());
        Vector2f offsetToParent = getOffsetToParent(index, getParent());

        setSizeToParent(sizeToParent.x, sizeToParent.y);
        setOffsetToParent(offsetToParent.x, offsetToParent.y);
    }

    ArrayList<UiBackgroundElement> getSettingElements() {
        return settingElements;
    }

    ShapePlaceable getPlaceable() {
        return placeable;
    }

    private Clickable getAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            ((ShapesTab) getParent()).setSelectedDisplay(this);
            for (UiBackgroundElement settingElement : ((ShapesTab) getParent()).getShapePlaceableSettingSliders()) settingElement.setVisible(false);
            for (UiBackgroundElement settingElement : settingElements) settingElement.setVisible(true);
            ((ShapesTab) getParent()).refreshShapePreview();
            return ButtonResult.SUCCESS;
        };
    }

    private static Vector2f getSizeToParent(Renderable parent) {
        return new Vector2f(0.0475F, 0.0475F * Window.getAspectRatio() * parent.getAspectRatio());
    }

    private static Vector2f getOffsetToParent(int index, Renderable parent) {
        int row = index / 6, column = index % 6;
        return new Vector2f(0.025F + column * 0.05F, 0.875F - (row + 1) * 0.05F * Window.getAspectRatio() * parent.getAspectRatio());
    }

    private final ArrayList<UiBackgroundElement> settingElements = new ArrayList<>();
    private final ShapePlaceable placeable;
    private final int index;
}
