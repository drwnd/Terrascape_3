package game.player.inventory;

import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.utils.StringGetter;
import org.joml.Vector2f;

public final class TabOpenerButton extends UiButton {

    static final float SIZE = 0.075F;

    TabOpenerButton(int index, Clickable clickable, StringGetter name) {
        super(new Vector2f(SIZE, SIZE * Window.getAspectRatio()), new Vector2f(0.0F, 1.0F - (index + 1) * SIZE * Window.getAspectRatio()), clickable);
        setScaleWithGuiSize(false);
        setRimThicknessMultiplier(0.75F);
        TextElement textElement = new TextElement(new Vector2f(0.1F, 0.5F), name);
        textElement.setDoAutoFocusScaling(false);
        addRenderable(textElement);
    }
}
