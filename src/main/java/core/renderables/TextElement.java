package core.renderables;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.rendering_api.Window;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.OptionSetting;
import core.settings.optionSettings.FontOption;
import core.utils.Message;
import core.utils.StringGetter;

import org.joml.Vector2f;

import java.awt.*;

public final class TextElement extends Renderable {

    public TextElement(Vector2f offsetToParent) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        text = new Message("");
    }

    public TextElement(Vector2f offsetToParent, StringGetter text) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.text = text;
    }

    public TextElement(Vector2f sizeToParent, Vector2f offsetToParent, StringGetter text) {
        super(sizeToParent, offsetToParent);
        this.text = text;
    }

    public TextElement(Vector2f offsetToParent, StringGetter text, Color color) {
        super(new Vector2f(1.0f, 1.0f), offsetToParent);
        this.text = text;
        this.color = color;
    }


    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        String text = this.text.get();

        float textSize = FloatSetting.TEXT_SIZE.value();
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;
        float charWidth = Window.getWidth() * defaultTextSize.x * textSize;
        float charHeight = Window.getHeight() * defaultTextSize.y * textSize;

        float maxAllowedLength = getParent().getPosition().x + getParent().getSize().x - position.x;
        int maxLength = TextShader.getMaxLength(text, maxAllowedLength, defaultTextSize.x, scalesWithGuiSize());

        position = new Vector2f(position.x, position.y - defaultTextSize.y * textSize / guiSize * 0.5f);
        TextShader textShader = (TextShader) AssetManager.get(CoreShaders.TEXT);
        textShader.bind();
        textShader.setUniform("screenSize", Window.getWidth(), Window.getHeight());
        textShader.setUniform("charSize", charWidth, charHeight);
        textShader.drawText(position, text.substring(0, maxLength), color, addTransparentBackground, scalesWithGuiSize());
    }

    public float getLength() {
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        return TextShader.getTextLength(text.get(), defaultTextSize.x, scalesWithGuiSize());
    }

    public String getText() {
        return text.get();
    }

    public void setText(StringGetter text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = new Message(text);
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setAddTransparentBackground(boolean addTransparentBackground) {
        this.addTransparentBackground = addTransparentBackground;
    }

    private boolean addTransparentBackground = false;
    private Color color = Color.WHITE;
    private StringGetter text;
}
