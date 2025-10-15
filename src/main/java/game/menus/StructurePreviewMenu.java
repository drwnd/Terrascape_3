package game.menus;

import core.languages.UiMessage;
import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiBackgroundElement;
import core.renderables.UiButton;
import core.rendering_api.Window;

import game.player.rendering.StructureDisplay;
import game.server.generation.Structure;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class StructurePreviewMenu extends UiBackgroundElement {

    public StructurePreviewMenu(Structure structure) {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));
        display = new StructureDisplay(new Vector2f(), new Vector2f(), structure);
        display.setAllowFocusScaling(false);
        display.setScaleWithGuiSize(false);

        UiButton backButton = new UiButton(new Vector2f(0.25f, 0.1f), new Vector2f(0.05f, 0.85f), getBackButtonAction());
        backButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), UiMessage.BACK));

        addRenderable(backButton);
        addRenderable(display);
    }

    public void changeZoom(boolean zoomIn) {
        display.changeZoom(zoomIn ? 1.05f : 1 / 1.05f);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);

        display.setSizeToParent(1.0f, Window.getAspectRatio());
        display.setOffsetToParent(0.0f, 0.5f - Window.getAspectRatio() * 0.5f);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new StructurePreviewMenuInput(this));
    }

    private static Clickable getBackButtonAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.popRenderable();
        };
    }

    private final StructureDisplay display;
}
