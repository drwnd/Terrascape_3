package core.renderables;

import core.assets.CoreSounds;
import core.settings.CoreFloatSettings;
import core.sound.Sound;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class UiButton extends UiBackgroundElement {

    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Runnable runnable) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> {
            runnable.run();
            return ButtonResult.SUCCESS;
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
            return ButtonResult.SUCCESS;
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
            return ButtonResult.SUCCESS;
        };
    }

    public Clickable getClickable() {
        return clickable;
    }

    @Override
    public boolean clickOn(Vector2i cursorPos, int button, int action) {
        ButtonResult result = clickable.clickOn(cursorPos, button, action);
        if (result == ButtonResult.SUCCESS) Sound.playUI(CoreSounds.BUTTON_SUCCESS, CoreFloatSettings.UI_AUDIO);
        if (result == ButtonResult.FAILURE) Sound.playUI(CoreSounds.BUTTON_FAILURE, CoreFloatSettings.UI_AUDIO);
        return result == ButtonResult.SUCCESS;
    }

    private Clickable clickable;
}
