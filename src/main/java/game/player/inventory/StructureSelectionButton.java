package game.player.inventory;


import core.renderables.TextElement;
import core.renderables.UiButton;
import core.utils.Message;

import game.assets.StructureIdentifier;

import org.joml.Vector2f;

public final class StructureSelectionButton extends UiButton {

    public StructureSelectionButton(Vector2f sizeToParent, Vector2f offsetToParent, String structureName) {
        super(sizeToParent, offsetToParent);
        identifier = new StructureIdentifier(structureName);
        addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), new Message(structureName)));
    }

    public StructureIdentifier getStructure() {
        return identifier;
    }

    private final StructureIdentifier identifier;
}
