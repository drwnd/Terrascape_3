package core.renderables;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.assets.Texture;
import core.assets.identifiers.TextureIdentifier;
import core.rendering_api.shaders.GuiShader;

import core.settings.optionSettings.TexturePack;
import org.joml.Vector2f;

public class UiElement extends Renderable {
    public UiElement(Vector2f sizeToParent, Vector2f offsetToParent, TextureIdentifier texture) {
        super(sizeToParent, offsetToParent);
        this.texture = texture;
    }

    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        Texture background = AssetManager.get(TexturePack.get(texture));
        shader.bind();
        if (scalesWithGuiSize()) shader.drawQuad(position, size, background);
        else shader.drawQuadNoGuiScale(position, size, background);
    }

    private final TextureIdentifier texture;
}
