package game.menus;

import core.renderables.CoreSettingsRenderable;
import core.renderables.OptionToggle;
import core.renderables.TextElement;
import core.utils.Message;

import game.player.rendering.DebugScreenLine;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class SettingsRenderable extends CoreSettingsRenderable {

    public void addDebugLineSetting(DebugScreenLine debugLine) {
        settingsCount++;

        Vector2f sizeToParent = new Vector2f(0.15f, 0.1f);
        float yOffset = 1.0f - 0.15f * settingsCount;

        TextElement nameDisplay = new TextElement(new Vector2f(0.225f, 0), new Vector2f(0.375f, yOffset + 0.05f), new Message(debugLine.name()));
        nameDisplay.setAllowFocusScaling(false);
        OptionToggle colorOption = new OptionToggle(sizeToParent, new Vector2f(0.6f, yOffset), debugLine.color(), null);
        OptionToggle visibilityOption = new OptionToggle(sizeToParent, new Vector2f(0.8f, yOffset), debugLine.visibility(), null);

        addRenderable(nameDisplay);
        addRenderable(colorOption);
        addRenderable(visibilityOption);

        movingRenderables.add(nameDisplay);
        options.add(colorOption);
        options.add(visibilityOption);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            colorOption.setToDefault();
            visibilityOption.setToDefault();
        });
    }
}
