package core.renderables;

import core.assets.CoreSounds;
import core.rendering_api.Debug;
import core.settings.CoreFloatSettings;
import core.sound.Sound;
import core.utils.MainThread;
import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class UiButton extends UiBackgroundElement {

    @MainThread
    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Runnable runnable) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            runnable.run();
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent, Clickable clickable) {
        super(sizeToParent, offsetToParent);
        this.clickable = clickable;
    }

    @MainThread
    public UiButton(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        clickable = (_, _, _) -> {
            Debug.err("No action set for this button %s%n", this);
            return ButtonResult.SUCCESS;
        };
    }

    public void setAction(Clickable clickable) {
        if (clickable == null) return;
        this.clickable = clickable;
    }

    public void setAction(Runnable action) {
        if (action == null) return;
        clickable = (_, _, buttonAction) -> {
            if (buttonAction != GLFW_PRESS) return ButtonResult.IGNORE;
            action.run();
            return ButtonResult.SUCCESS;
        };
    }

    public Clickable getClickable() {
        return clickable;
    }

    @Override
    @MainThread
    public boolean clickOn(Vector2i cursorPos, int button, int action) {
        ButtonResult result = clickable.clickOn(cursorPos, button, action);
        if (result == ButtonResult.SUCCESS) Sound.playUI(CoreSounds.BUTTON_SUCCESS, CoreFloatSettings.UI_AUDIO);
        if (result == ButtonResult.FAILURE) Sound.playUI(CoreSounds.BUTTON_FAILURE, CoreFloatSettings.UI_AUDIO);
        return result == ButtonResult.SUCCESS;
    }

    private Clickable clickable;
}
