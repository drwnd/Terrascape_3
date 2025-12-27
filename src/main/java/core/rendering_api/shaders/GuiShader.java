package core.rendering_api.shaders;

import core.assets.AssetManager;
import core.assets.GuiElement;
import core.assets.identifiers.ShaderIdentifier;
import core.assets.Texture;
import core.assets.CoreGuiElements;
import core.settings.FloatSetting;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

public final class GuiShader extends RenderShader {
    public GuiShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }

    @Override
    public void bind() {
        GL46.glUseProgram(programID);
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glDepthMask(true);
    }

    public void flipNextDrawVertically() {
        flipNextDrawVertically = true;
    }

    public void drawQuad(Vector2f position, Vector2f size, Texture texture) {
        drawQuadCustomScale(position, size, texture, FloatSetting.GUI_SIZE.value());
    }

    public void drawQuadNoGuiScale(Vector2f position, Vector2f size, Texture texture) {
        drawQuadCustomScale(position, size, texture, 1.0F);
    }

    public void drawQuadCustomScale(Vector2f position, Vector2f size, Texture texture, float scale) {
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, texture.getID());

        setUniform("image", 0);
        if (flipNextDrawVertically) {
            setUniform("position", (position.x - 0.5F) * scale, (position.y + size.y - 0.5F) * scale);
            setUniform("size", size.x * scale, -size.y * scale);
        } else {
            setUniform("position", (position.x - 0.5F) * scale, (position.y - 0.5F) * scale);
            setUniform("size", size.x * scale, size.y * scale);
        }

        draw();
    }

    public void drawFullScreenQuad() {
        if (flipNextDrawVertically) {
            setUniform("position", -0.5F, 0.5F);
            setUniform("size", 1.0F, -1.0F);
        } else {
            setUniform("position", -0.5F, -0.5F);
            setUniform("size", 1.0F, 1.0F);
        }

        draw();
    }


    private void draw() {
        GuiElement quad = AssetManager.get(CoreGuiElements.QUAD);

        GL46.glBindVertexArray(quad.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, quad.getVertexCount());
        flipNextDrawVertically = false;
    }

    private boolean flipNextDrawVertically = false;
}
