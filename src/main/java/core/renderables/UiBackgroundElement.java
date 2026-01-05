package core.renderables;

import core.assets.AssetManager;
import core.assets.Texture;
import core.assets.CoreShaders;
import core.assets.CoreTextures;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.settings.FloatSetting;

import core.settings.optionSettings.TexturePack;
import org.joml.Vector2f;

public class UiBackgroundElement extends Renderable {
    public UiBackgroundElement(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
    }

    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;

        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI_BACKGROUND);
        Texture background = AssetManager.get(TexturePack.get(CoreTextures.GUI_ELEMENT_BACKGROUND));
        shader.bind();
        shader.setUniform("rimWidth", FloatSetting.RIM_THICKNESS.value() * rimThicknessMultiplier * guiSize);
        shader.setUniform("aspectRatio", Window.getAspectRatio());
        shader.drawQuadCustomScale(position, size, background, guiSize);
    }

    public void setRimThicknessMultiplier(float rimThicknessMultiplier) {
        this.rimThicknessMultiplier = rimThicknessMultiplier;
    }

    protected float getRimThicknessMultiplier() {
        return rimThicknessMultiplier;
    }

    private float rimThicknessMultiplier = 1.0f;
}
