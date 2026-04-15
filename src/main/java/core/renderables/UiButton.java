package core.renderables;

import core.assets.CoreSounds;
import core.sound.Sound;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class UiButton extends UiBackgroundElement {

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Runnable runnable) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> {
            runnable.run();
            return true;
        };
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Clickable clickable) {
        super(sizeToParent, offsetToParent);
        this.clickable = clickable;
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> {
            System.err.printf("No action set for this button %s%n", this);
            return true;
        };
    }

    public void setAction(Clickable clickable) {
        if (clickable == null) return;
        this.clickable = clickable;
    }

    public void setAction(Runnable action) {
        if (action == null) return;
        clickable = (_, _, _) -> {
            action.run();
            return true;
        };
    }

    public Clickable getClickable() {
        return clickable;
    }

    @Override
    public boolean clickOn(Vector2i cursorPos, int button, int action) {
        boolean success = clickable.clickOn(cursorPos, button, action);
        if (success) Sound.playUI(CoreSounds.CLICK, null);
        return success;
    }

    private Clickable clickable;
}
