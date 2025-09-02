package renderables;

import assets.AssetManager;
import assets.identifiers.ShaderIdentifier;
import org.joml.Vector2f;
import rendering_api.Window;
import rendering_api.shaders.TextShader;
import settings.FloatSetting;

import java.awt.*;

public class TextElement extends Renderable {

    public static final float DEFAULT_TEXT_SCALAR = 2;
    public static final Vector2f DEFAULT_TEXT_SIZE = new Vector2f(7f / 1920f, 13f / 1080f).mul(DEFAULT_TEXT_SCALAR);

    public TextElement(Vector2f offsetToParent) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.charSize = new Vector2f(1.0f, 1.0f);
    }

    public TextElement(Vector2f offsetToParent, String text) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.charSize = new Vector2f(1.0f, 1.0f);
        this.text = text;
    }

    public TextElement(Vector2f sizeToParent, Vector2f offsetToParent, String text) {
        super(sizeToParent, offsetToParent);
        this.charSize = new Vector2f(1.0f, 1.0f);
        this.text = text;
    }

    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        float charSizeX = charSize.x * DEFAULT_TEXT_SIZE.x;
        float charSizeY = charSize.y * DEFAULT_TEXT_SIZE.y;

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = FloatSetting.GUI_SIZE.value();
        float charWidth = Window.getWidth() * charSizeX * textSize;
        float charHeight = Window.getHeight() * charSizeY * textSize;

        float maxAllowedLength = getParent().getPosition().x + getParent().getSize().x - position.x;
        int maxLength = TextShader.getMaxLength(text, maxAllowedLength, charSize.x);

        position = new Vector2f(position.x, position.y - charSizeY * textSize / guiSize * 0.5f);
        TextShader textShader = (TextShader) AssetManager.getShader(ShaderIdentifier.TEXT);
        textShader.bind();
        textShader.setUniform("screenSize", Window.getWidth(), Window.getHeight());
        textShader.setUniform("charSize", charWidth, charHeight);
        textShader.drawText(position, text.substring(0, maxLength), Color.WHITE, false, true);
    }

    public void setText(String text) {
        this.text = text;
    }

    private String text = "";
    private final Vector2f charSize;
}
