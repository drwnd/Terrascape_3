package core.rendering_api.shaders;

import core.assets.AssetManager;
import core.assets.CoreBuffers;
import core.assets.CoreVertexArrays;
import core.assets.identifiers.*;
import core.settings.FloatSetting;
import core.settings.OptionSetting;
import core.settings.optionSettings.FontOption;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

import java.awt.*;

public class TextShader extends RenderShader {
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
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0F;

        setUniform("string", toIntFormat(text));
        setUniform("offsets", getOffsets(text));
        setUniform("position", (position.x - 0.5F) * guiSize, (position.y - 0.5F) * guiSize);
        setUniform("color", color);
        setUniform("textAtlas", 0);
        setUniform("addTransparentBackground", addTransparentBackground);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.get((TextureIdentifier) OptionSetting.FONT.value()).getID());
        GL46.glBindVertexArray(AssetManager.get(CoreVertexArrays.TEXT_ROW).getID());
        GL46.glEnableVertexAttribArray(0);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, AssetManager.get(CoreBuffers.TEXT_ELEMENT_ARRAY_BUFFER).getID());

        GL46.glDrawElements(GL46.GL_TRIANGLES, 6 * MAX_TEXT_LENGTH, GL46.GL_UNSIGNED_INT, 0);
    }

    public static int getMaxLength(String text, float maxAllowedLength, float charWidth, boolean scalesWithGuiSize) {
        int[] offsets = getOffsets(text);

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0F;
        float factor = textSize * charWidth / (guiSize * 7);

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++)
            if (offsets[index + 1] * factor > maxAllowedLength) return index;
        return text.length();
    }

    public static float getTextLength(String text, float charWidth, boolean scalesWithGuiSize) {
        int[] offsets = getOffsets(text);

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0F;
        float factor = textSize * charWidth / (guiSize * 7);

        return offsets[Math.min(MAX_TEXT_LENGTH, text.length())] * factor;
    }

    private static int[] toIntFormat(String text) {
        int[] array = new int[MAX_TEXT_LENGTH];
        char[] chars = text.toCharArray();

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++)
            array[index] = getCharIndex(chars[index]);

        return array;
    }

    private static int[] getOffsets(String text) {
        int[] array = new int[MAX_TEXT_LENGTH + 1];
        char[] chars = text.toCharArray();
        byte[] charSizes = ((FontOption) OptionSetting.FONT.value()).getCharSizes();
        array[0] = 0;
        int max = Math.min(text.length(), MAX_TEXT_LENGTH);

        for (int index = 0; index < max; index++)
            array[index + 1] = array[index] + getCharWidth(chars[index], charSizes) + CHAR_PADDING;
        for (int index = max; index < MAX_TEXT_LENGTH; index++) array[index + 1] = array[index];

        return array;
    }

    private static int getCharWidth(char character, byte[] charSizes) {
        return charSizes[getCharIndex(character)];
    }

    private static int getCharIndex(char character) {
        return (character & 0xFF) == character ? character : 0;
    }

    private static final int CHAR_PADDING = 1;
}
