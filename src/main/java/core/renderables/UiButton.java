package core.renderables;

import core.assets.CoreSounds;
import core.sound.Sound;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class UiButton extends UiBackgroundElement implements Clickable {

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Runnable runnable) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> runnable.run();
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Clickable clickable) {
        super(sizeToParent, offsetToParent);
        this.clickable = clickable;
    }

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> System.err.printf("No action set for this button %s%n", this);
    }

    public void setAction(Clickable clickable) {
        if (clickable == null) return;
        this.clickable = clickable;
    }

    public void setAction(Runnable action) {
        if (action == null) return;
        clickable = (_, _, _) -> action.run();
    }

    public Clickable getClickable() {
        return clickable;
    }

    @Override
    public void clickOn(Vector2i cursorPos, int button, int action) {
        Sound.playUI(CoreSounds.CLICK, null);
        clickable.clickOn(cursorPos, button, action);
    }

    private Clickable clickable;
}
