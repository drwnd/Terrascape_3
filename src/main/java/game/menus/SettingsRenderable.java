package game.menus;

import core.renderables.ButtonResult;
import core.renderables.CoreSettingsRenderable;
import core.renderables.OptionToggle;
import core.renderables.TextElement;
import core.utils.Message;

import game.settings.DebugScreenOptions;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class SettingsRenderable extends CoreSettingsRenderable {

    public void addDebugLineSetting(DebugScreenOptions debugLine) {
        settingsCount++;

        Vector2f sizeToParent = new Vector2f(0.15F, 0.1F);
        float yOffset = 1.0F - 0.15F * settingsCount;

        TextElement nameDisplay = new TextElement(new Vector2f(0.225F, 0), new Vector2f(0.375F, yOffset + 0.05F), new Message(debugLine.name()));
        nameDisplay.setDoAutoFocusScaling(false);
        OptionToggle colorOption = new OptionToggle(sizeToParent, new Vector2f(0.6F, yOffset), debugLine.getColor(), null, false);
        OptionToggle visibilityOption = new OptionToggle(sizeToParent, new Vector2f(0.8F, yOffset), debugLine.getVisibility(), null, false);

        addRenderable(nameDisplay);
        addRenderable(colorOption);
        addRenderable(visibilityOption);

        movingRenderables.add(nameDisplay);
        options.add(colorOption);
        options.add(visibilityOption);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            colorOption.setToDefault();
            visibilityOption.setToDefault();
            return ButtonResult.SUCCESS;
        });
    }
}
