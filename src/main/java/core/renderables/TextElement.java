package core.renderables;

import core.assets.AssetManager;
import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.Window;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;

import org.joml.Vector2f;

import java.awt.*;

public class TextElement extends Renderable {

    public static final float DEFAULT_TEXT_SCALAR = 2;
    public static final Vector2f DEFAULT_TEXT_SIZE = new Vector2f(7f / 1920f, 13f / 1080f).mul(DEFAULT_TEXT_SCALAR);

    public TextElement(Vector2f offsetToParent) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
    }

    public TextElement(Vector2f offsetToParent, String text) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.text = text;
    }

    public TextElement(Vector2f sizeToParent, Vector2f offsetToParent, String text) {
        super(sizeToParent, offsetToParent);
        this.text = text;
    }

    public TextElement(Vector2f offsetToParent, String text, Color color) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.text = text;
        this.color = color;
    }


    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        float charSizeX = DEFAULT_TEXT_SIZE.x;
        float charSizeY = DEFAULT_TEXT_SIZE.y;

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;
        float charWidth = Window.getWidth() * charSizeX * textSize;
        float charHeight = Window.getHeight() * charSizeY * textSize;

        float maxAllowedLength = getParent().getPosition().x + getParent().getSize().x - position.x;
        int maxLength = TextShader.getMaxLength(text, maxAllowedLength, 1, scalesWithGuiSize());

        position = new Vector2f(position.x, position.y - charSizeY * textSize / guiSize * 0.5f);
        TextShader textShader = (TextShader) AssetManager.getShader(ShaderIdentifier.TEXT);
        textShader.bind();
        textShader.setUniform("screenSize", Window.getWidth(), Window.getHeight());
        textShader.setUniform("charSize", charWidth, charHeight);
        textShader.drawText(position, text.substring(0, maxLength), color, false, true);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    private Color color = Color.WHITE;
    private String text = "";
}
