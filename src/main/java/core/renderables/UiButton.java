package core.renderables;

import org.joml.Vector2f;
import org.joml.Vector2i;

public class UiButton extends UiBackgroundElement implements Clickable {

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Runnable action) {
        super(sizeToParent, offsetToParent);
        this.action = (cursorPos, button, buttonAction) -> action.run();
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Clickable action) {
        super(sizeToParent, offsetToParent);
        this.action = action;
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        this.action = (cursorPos, button, action) -> System.err.printf("No action set for this button %s%n", this);
    }

    public void setAction(Clickable action) {
        if (action == null) return;
        this.action = action;
    }

    public void setAction(Runnable action) {
        if (action == null) return;
        this.action = (cursorPos, button, buttonAction) -> action.run();
    }

    @Override
    public void clickOn(Vector2i cursorPos, int button, int action) {
        this.action.clickOn(cursorPos, button, action);
    }

    private Clickable action;
}
