package core.renderables;

import core.assets.AssetManager;
import core.assets.Texture;
import core.assets.identifiers.ShaderIdentifier;
import core.assets.identifiers.TextureIdentifier;
import core.rendering_api.shaders.GuiShader;
import core.settings.ToggleSetting;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class Toggle extends UiButton {

    public Toggle(Vector2f sizeToParent, Vector2f offsetToParent, ToggleSetting setting, StringGetter settingName) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());

        this.setting = setting;

        matchSetting();
        addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), settingName));
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

        position = new Vector2f(position).add(0.785f * size.x, 0.15f * size.y);
        size = new Vector2f(size).mul(0.2f, 0.7f);

        GuiShader shader = (GuiShader) AssetManager.getShader(ShaderIdentifier.GUI);
        Texture texture = AssetManager.getTexture(value ? TextureIdentifier.TOGGLE_ACTIVATED : TextureIdentifier.TOGGLE_DEACTIVATED);
        shader.bind();
        shader.drawQuad(position, size, texture);
    }

    public void matchSetting() {
        value = setting.value();
    }


    private Clickable getAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action == GLFW.GLFW_PRESS) value = !value;
        };
    }

    private boolean value;
    private final ToggleSetting setting;
}
