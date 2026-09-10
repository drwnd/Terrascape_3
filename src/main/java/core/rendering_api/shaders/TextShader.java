package core.rendering_api.shaders;

import core.assets.*;
import core.assets.identifiers.*;
import core.rendering_api.Window;
import core.settings.CoreFloatSettings;
import core.settings.CoreOptionSettings;
import core.settings.optionSettings.FontOption;

import org.joml.Vector2f;

import static org.lwjgl.opengl.GL46.*;

import java.awt.*;

public final class TextShader extends RenderShader {
    public static final int MAX_TEXT_LENGTH = 128;

    public TextShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }

    @Override
    public void bind() {
        glUseProgram(programID);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void drawText(Vector2f position, String text, Color color, boolean addTransparentBackground, boolean scalesWithGuiSize) {
        drawText(position, text, 1, 1 - position.x, color, addTransparentBackground, scalesWithGuiSize);
    }

    public void drawText(Vector2f position, String text, float textSize, float maxAllowedLength, Color color, boolean addTransparentBackground, boolean scalesWithGuiSize) {
        float guiSize = scalesWithGuiSize ? CoreFloatSettings.GUI_SIZE.value() : 1.0F;
        FontOption font = (FontOption) CoreOptionSettings.FONT.value();
        int textLength = intoArrays(text, font, maxAllowedLength / textSize, guiSize);

        setUniform("charSize", font.getDefaultTextSize().mul(textSize));
        setUniform("string", textChars);
        setUniform("offsets", offsets);
        setUniform("position", (position.x - 0.5F) * guiSize, (position.y - 0.5F) * guiSize);
        setUniform("color", color);
        setUniform("textAtlas", 0);
        setUniform("addTransparentBackground", addTransparentBackground);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, AssetManager.get(font).id());
        glBindVertexArray(AssetManager.get(CoreVertexArrays.TEXT_ROW).id());
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, AssetManager.get(CoreBuffers.TEXT_ELEMENT_ARRAY_BUFFER).id());

        glDrawElements(GL_TRIANGLES, 6 * textLength, GL_UNSIGNED_INT, 0);
    }


    public static float getTextLength(String text, float charWidth, boolean scalesWithGuiSize) {
        FontOption font = (FontOption) CoreOptionSettings.FONT.value();
        char[] chars = text.toCharArray();
        byte[] charSizes = font.getCharSizes();
        float normalizer = 2 / font.getDefaultTextSize().x / Window.getWidth();

        float offset = 0;
        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++)
            offset += (getCharWidth(chars[index], charSizes) + CHAR_PADDING) * normalizer;

        float textSize = CoreFloatSettings.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize ? CoreFloatSettings.GUI_SIZE.value() : 1.0F;
        float factor = textSize * charWidth / guiSize;

        return offset * factor;
    }


    private int intoArrays(String text, FontOption font, float maxAllowedLength, float guiSize) {
        char[] chars = text.toCharArray();
        int max = Math.min(text.length(), MAX_TEXT_LENGTH);

        byte[] charSizes = font.getCharSizes();
        float normalizer = 2 / font.getDefaultTextSize().x / Window.getWidth();
        float offsetFactor = CoreFloatSettings.TEXT_SIZE.value() * font.getDefaultTextSize().x / guiSize;
        offsets[0] = 0;

        for (int index = 0; index < max; index++) {
            textChars[index] = getCharIndex(chars[index]);
            offsets[index + 1] = offsets[index] + (getCharWidth(chars[index], charSizes) + CHAR_PADDING) * normalizer;
            if (offsets[index + 1] * offsetFactor > maxAllowedLength) return index;
        }
        return max;
    }

    private static int getCharWidth(char character, byte[] charSizes) {
        return charSizes[getCharIndex(character)];
    }

    private static int getCharIndex(char character) {
        return (character & 0xFF) == character ? character : 0;
    }

    private final int[] textChars = new int[MAX_TEXT_LENGTH];
    private final float[] offsets = new float[MAX_TEXT_LENGTH + 1];

    private static final int CHAR_PADDING = 1;
}
