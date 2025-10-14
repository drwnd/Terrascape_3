package game.player.rendering;

import core.assets.AssetGenerator;
import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.utils.Message;
import game.menus.StructurePreviewMenu;
import game.server.generation.Structure;
import game.server.saving.StructureSaver;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class StructureSelectionButton extends UiButton implements AssetIdentifier<Structure> {

    public StructureSelectionButton(Vector2f sizeToParent, Vector2f offsetToParent, String structureName) {
        super(sizeToParent, offsetToParent);
        this.structureName = structureName;
        setAction(getAction());

        addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), new Message(structureName)));
    }

    public Structure getStructure() {
        return AssetManager.get(this);
    }

    @Override
    public AssetGenerator<Structure> getAssetGenerator() {
        return () -> new StructureSaver().load(StructureSaver.getSaveFileLocation(structureName));
    }

    private Clickable getAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.pushRenderable(new StructurePreviewMenu(getStructure()));
        };
    }

    private final String structureName;
}
