package game.player.rendering;

import core.assets.AssetManager;
import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.utils.Message;

import game.assets.StructureIdentifier;
import game.menus.StructurePreviewMenu;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class StructureSelectionButton extends UiButton {

    public StructureSelectionButton(Vector2f sizeToParent, Vector2f offsetToParent, String structureName) {
        super(sizeToParent, offsetToParent);
        identifier = new StructureIdentifier(structureName);
        setAction(getAction());

        addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), new Message(structureName)));
    }

    public StructureIdentifier getStructure() {
        return identifier;
    }

    private Clickable getAction() {
        return (Vector2i _, int button, int action) -> {
            if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT || action != GLFW_PRESS) return;
            Window.pushRenderable(new StructurePreviewMenu(AssetManager.get(identifier)));
        };
    }

    private final StructureIdentifier identifier;
}
