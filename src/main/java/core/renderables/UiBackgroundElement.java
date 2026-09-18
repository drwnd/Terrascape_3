package core.renderables;

import core.assets.AssetManager;
import core.assets.Texture;
import core.assets.CoreShaders;
import core.assets.CoreTextures;
import core.assets.identifiers.AssetIdentifier;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.settings.CoreFloatSettings;

import core.utils.MainThread;
import org.joml.Vector2f;

import java.awt.*;

public class UiBackgroundElement extends Renderable {

    @MainThread
    public UiBackgroundElement(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
    }

    @Override
    @MainThread
    protected void renderSelf(Vector2f position, Vector2f size) {
        float guiSize = scalesWithGuiSize() ? CoreFloatSettings.GUI_SIZE.value() : 1.0F;

        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI_BACKGROUND);
        Texture background = AssetManager.get(textureIdentifier);
        shader.bind();
        shader.setUniform("rimWidth", CoreFloatSettings.RIM_THICKNESS.value() * rimThicknessMultiplier * guiSize);
        shader.setUniform("aspectRatio", Window.getAspectRatio());
        shader.setUniform("colorMultiplier", colorMultiplier);
        shader.drawQuadCustomScale(position, size, background, guiSize);
    }

    public void setRimThicknessMultiplier(float rimThicknessMultiplier) {
        this.rimThicknessMultiplier = rimThicknessMultiplier;
    }

    protected float getRimThicknessMultiplier() {
        return rimThicknessMultiplier;
    }

    public void setColorMultiplier(Color colorMultiplier) {
        if (colorMultiplier != null) this.colorMultiplier = colorMultiplier;
    }

    public void setTextureIdentifier(AssetIdentifier<Texture> textureIdentifier) {
        if (textureIdentifier != null) this.textureIdentifier = textureIdentifier;
    }

    private float rimThicknessMultiplier = 1.0F;
    private Color colorMultiplier = Color.WHITE;
    private AssetIdentifier<Texture> textureIdentifier = CoreTextures.GUI_ELEMENT_BACKGROUND;
}
