package core.rendering_api.shaders;

import core.assets.AssetManager;
import core.assets.identifiers.BufferIdentifier;
import core.assets.identifiers.ShaderIdentifier;
import core.assets.identifiers.TextureIdentifier;
import core.assets.identifiers.VertexArrayIdentifier;
import core.renderables.TextElement;
import core.rendering_api.Window;
import core.settings.FloatSetting;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

import java.awt.*;
import java.nio.charset.StandardCharsets;

public class TextShader extends Shader {
    public static final int MAX_TEXT_LENGTH = 128;

    public TextShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }

    @Override
    public void bind() {
        GL46.glUseProgram(programID);
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void drawText(Vector2f position, String text, Color color, boolean addTransparentBackground, boolean scalesWithGuiSize) {
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0f;

        setUniform("string", toIntFormat(text));
        setUniform("offsets", getOffsets(text));
        setUniform("position", (position.x - 0.5f) * guiSize, (position.y - 0.5f) * guiSize);
        setUniform("color", color);
        setUniform("textAtlas", 0);
        setUniform("addTransparentBackground", addTransparentBackground);

        GL46.glActiveTexture(0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.TEXT).getID());
        GL46.glBindVertexArray(AssetManager.getVertexArray(VertexArrayIdentifier.TEXT_ROW).getID());
        GL46.glEnableVertexAttribArray(0);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, AssetManager.getBuffer(BufferIdentifier.MODEL_INDEX).getID());

        GL46.glDrawElements(GL46.GL_TRIANGLES, 768, GL46.GL_UNSIGNED_INT, 0);
    }

    public static int getMaxLength(String text, float maxAllowedLength, float charWidth, boolean scalesWithGuiSize) {
        int[] offsets = getOffsets(text);

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0f;
        float factor = TextElement.DEFAULT_TEXT_SCALAR * textSize * charWidth / (Window.getWidth() * guiSize);

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++)
            if (offsets[index + 1] * factor > maxAllowedLength) return index;
        return text.length();
    }

    private static int[] toIntFormat(String text) {
        int[] array = new int[MAX_TEXT_LENGTH];
        byte[] stringBytes = text.getBytes(StandardCharsets.UTF_8);

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++)
            array[index] = stringBytes[index];

        return array;
    }

    private static int[] getOffsets(String text) {
        int[] array = new int[MAX_TEXT_LENGTH + 1];
        char[] chars = text.toCharArray();
        array[0] = 0;
        int max = Math.min(text.length(), MAX_TEXT_LENGTH);

        for (int index = 0; index < max; index++)
            array[index + 1] = array[index] + getCharWidth(chars[index]) + CHAR_PADDING;
        for (int index = max; index < MAX_TEXT_LENGTH; index++) array[index + 1] = array[index];

        return array;
    }

    private static int getCharWidth(char character) {
        return switch (character) {
            case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 1;
            case '[', ']', '`', 'j', 'l' -> 2;
            case '"', '(', ')', '*', '+', '-', 'I', '/', '1', '<', '=', '>', '\\', 'r', 't', '{', '}' -> 3;
            case '2', '4', '7', '?', 'C', 'E', 'F', 'J', 'K', 'L', '_', 'f', 'k' -> 4;
            case '@', 'Q' -> 6;
            default -> 5;
        };
    }

    private static final int CHAR_PADDING = 1;
}
