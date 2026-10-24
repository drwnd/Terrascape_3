package core.renderables;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.assets.Texture;
import core.assets.Texture2D;
import core.assets.identifiers.AssetIdentifier;
import core.rendering_api.shaders.GuiShader;

import core.utils.MainThread;
import org.joml.Vector2f;

public class UiElement extends Renderable {

    @MainThread
    public UiElement(Vector2f sizeToParent, Vector2f offsetToParent, AssetIdentifier<Texture2D> texture) {
        super(sizeToParent, offsetToParent);
        this.texture = texture;
    }

    @Override
    @MainThread
    protected void renderSelf(Vector2f position, Vector2f size) {
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        Texture background = AssetManager.get(texture);
        shader.bind();
        if (scalesWithGuiSize()) shader.drawQuad(position, size, background);
        else shader.drawQuadNoGuiScale(position, size, background);
    }

    private final AssetIdentifier<Texture2D> texture;
}
