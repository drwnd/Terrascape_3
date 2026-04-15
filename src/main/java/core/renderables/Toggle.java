package core.renderables;

import core.assets.AssetManager;
import core.assets.Texture;
import core.assets.CoreShaders;
import core.assets.CoreTextures;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.settings.CoreFloatSettings;
import core.settings.ToggleSetting;
import core.settings.optionSettings.TexturePack;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class Toggle extends UiButton {

    public Toggle(Vector2f sizeToParent, Vector2f offsetToParent, ToggleSetting setting, StringGetter settingName, boolean updateImmediately) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());

        this.setting = setting;
        this.updateImmediately = updateImmediately;

        matchSetting();
        addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), settingName));
    }

    public void setToDefault() {
        value = setting.defaultValue();
    }

    public ToggleSetting getSetting() {
        return setting;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);

        float guiSize = scalesWithGuiSize() ? CoreFloatSettings.GUI_SIZE.value() : 1.0F;
        float rimThickness = CoreFloatSettings.RIM_THICKNESS.value() * getRimThicknessMultiplier();
        float thicknessX = rimThickness / Window.getAspectRatio();
        float newSizeY = 1.0F - 2 * rimThickness / size.y;
        float oldSizeX = size.x;

        size = new Vector2f(size).mul(newSizeY / getAspectRatio(), newSizeY);
        position = new Vector2f(position).add(oldSizeX - size.x - thicknessX, rimThickness);

        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        Texture texture = AssetManager.get(TexturePack.get(value ? CoreTextures.TOGGLE_ACTIVATED : CoreTextures.TOGGLE_DEACTIVATED));
        shader.bind();
        shader.drawQuadCustomScale(position, size, texture, guiSize);
    }

    public void matchSetting() {
        value = setting.value();
    }


    private Clickable getAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return false;
            value = !value;
            if (updateImmediately) setting.setValue(value);
            return true;
        };
    }

    private boolean value;
    private final ToggleSetting setting;
    private final boolean updateImmediately;
}
