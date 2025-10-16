package game.menus;

import core.languages.UiMessage;
import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiBackgroundElement;
import core.renderables.UiButton;
import core.rendering_api.Input;
import core.rendering_api.Window;

import game.player.rendering.StructureDisplay;
import game.server.generation.Structure;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class StructurePreviewMenu extends UiBackgroundElement {

    public StructurePreviewMenu(Structure structure) {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        display = new StructureDisplay(new Vector2f(), new Vector2f(), structure);
        display.setAllowFocusScaling(false);
        display.setScaleWithGuiSize(false);

        UiButton backButton = new UiButton(new Vector2f(0.25F, 0.1F), new Vector2f(0.05F, 0.85F), getBackButtonAction());
        backButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.BACK));

        addRenderable(backButton);
        addRenderable(display);
    }

    public void changeZoom(boolean zoomIn) {
        display.changeZoom(zoomIn ? 1.05F : 1 / 1.05F);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);

        Vector2i cursorMovement = input.getCursorMovement();
        if (Input.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON)) display.rotate(cursorMovement);

        display.setSizeToParent(1.0F, Window.getAspectRatio());
        display.setOffsetToParent(0.0F, 0.5F - Window.getAspectRatio() * 0.5F);
    }

    @Override
    public void setOnTop() {
        Window.setInput(input);
    }

    private static Clickable getBackButtonAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.popRenderable();
        };
    }

    private final StructureDisplay display;
    private final StructurePreviewMenuInput input = new StructurePreviewMenuInput(this);
}
