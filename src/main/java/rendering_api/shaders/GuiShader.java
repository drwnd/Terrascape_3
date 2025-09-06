package rendering_api.shaders;

import assets.AssetManager;
import assets.GuiElement;
import assets.identifiers.GuiElementIdentifier;
import assets.Texture;
import assets.identifiers.ShaderIdentifier;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;
import settings.FloatSetting;

public class GuiShader extends Shader {
    public GuiShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }

    @Override
    public void bind() {
        GL46.glUseProgram(programID);
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
    }

    public void flipNextDrawVertically() {
        flipNextDrawVertically = true;
    }

    public void drawQuad(Vector2f position, Vector2f size, Texture texture) {
        drawQuadCustomScale(position, size, texture, FloatSetting.GUI_SIZE.value());
    }

    public void drawQuadNoGuiScale(Vector2f position, Vector2f size, Texture texture) {
        drawQuadCustomScale(position, size, texture, 1.0f);
    }

    public void drawQuadCustomScale(Vector2f position, Vector2f size, Texture texture, float scale) {
        GuiElement quad = AssetManager.getGuiElement(GuiElementIdentifier.QUAD);

        GL46.glBindVertexArray(quad.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, texture.getID());

        setUniform("image", 0);
        if (flipNextDrawVertically) {
            setUniform("position", (position.x - 0.5f) * scale, (position.y + size.y - 0.5f) * scale);
            setUniform("size", size.x * scale, -size.y * scale);
        } else {
            setUniform("position", (position.x - 0.5f) * scale, (position.y - 0.5f) * scale);
            setUniform("size", size.x * scale, size.y * scale);
        }

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, quad.getVertexCount());
        flipNextDrawVertically = false;
    }

    private boolean flipNextDrawVertically = false;
}
