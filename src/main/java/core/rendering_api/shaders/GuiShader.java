package core.rendering_api.shaders;

import core.assets.AssetManager;
import core.assets.GuiElement;
import core.assets.identifiers.ShaderIdentifier;
import core.assets.Texture;
import core.assets.CoreGuiElements;
import core.settings.FloatSetting;

import org.joml.Vector2f;

import static org.lwjgl.opengl.GL46.*;

public final class GuiShader extends Shader {
    public GuiShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }

    @Override
    public void bind() {
        glUseProgram(programID);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);
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
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture.getID());

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

        glBindVertexArray(quad.getVao());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_TRIANGLES, 0, quad.getVertexCount());
        flipNextDrawVertically = false;
    }

    private boolean flipNextDrawVertically = false;
}
