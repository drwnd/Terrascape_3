package core.renderables;

import game.assets.Textures;
import core.rendering_api.Window;
import core.utils.StringGetter;

import org.joml.Vector2f;

public final class TextField extends UiButton {

    public TextField(Vector2f sizeToParent, Vector2f offsetToParent, StringGetter name) {
        super(sizeToParent, offsetToParent);
        setAction(this::setOnTop);

        UiElement blackBox = new UiElement(new Vector2f(0.8f, 0.8f), new Vector2f(0.175f, 0.1f), Textures.INVENTORY_OVERLAY);
        textElement = new TextElement(new Vector2f(0.05f, 0.5f));
        blackBox.addRenderable(textElement);

        TextElement nameElement = new TextElement(new Vector2f(0.025f, 0.5f));
        nameElement.setText(name);

        addRenderable(blackBox);
        addRenderable(nameElement);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        textElement.setText(text);
        super.renderSelf(position, size);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new TextFieldInput(this));
    }

    private String text = "";
    private final TextElement textElement;
}
