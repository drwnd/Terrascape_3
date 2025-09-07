package renderables;

import assets.AssetManager;
import assets.Texture;
import assets.identifiers.ShaderIdentifier;
import assets.identifiers.TextureIdentifier;
import org.joml.Vector2f;
import rendering_api.Window;
import rendering_api.shaders.GuiShader;
import settings.FloatSetting;

public class UiBackgroundElement extends Renderable {
    public UiBackgroundElement(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
    }

    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;

        GuiShader shader = (GuiShader) AssetManager.getShader(ShaderIdentifier.GUI_BACKGROUND);
        Texture background = AssetManager.getTexture(TextureIdentifier.GUI_ELEMENT_BACKGROUND);
        shader.bind();
        shader.setUniform("rimWidth", FloatSetting.RIM_THICKNESS.value() * guiSize);
        shader.setUniform("aspectRatio", Window.getAspectRatio());
        shader.drawQuad(position, size, background);
    }
}
