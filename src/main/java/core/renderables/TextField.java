package core.renderables;

import core.assets.CoreTextures;
import core.rendering_api.Window;
import core.settings.FloatSetting;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class TextField extends UiButton {

    public TextField(Vector2f sizeToParent, Vector2f offsetToParent, StringGetter name) {
        this(sizeToParent, offsetToParent, name, () -> {
        });
    }

    public TextField(Vector2f sizeToParent, Vector2f offsetToParent, StringGetter name, Runnable onTextChange) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());
        this.onTextChange = onTextChange;

        textElement = new TextElement(new Vector2f());
        nameElement = new TextElement(new Vector2f());
        nameElement.setText(name);

        blackBox = new UiElement(new Vector2f(), new Vector2f(), CoreTextures.OVERLAY);
        blackBox.addRenderable(textElement);
        blackBox.addRenderable(nameElement);
        blackBox.setAllowFocusScaling(false);

        addRenderable(blackBox);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String oldText = this.text;
        this.text = text;
        if (!oldText.equals(text)) onTextChange.run();
        nameElement.setVisible(text.isEmpty());
    }


    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        textElement.setText(text);

        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;
        float rimThickness = FloatSetting.RIM_THICKNESS.value() * guiSize * getRimThicknessMultiplier();
        size = Window.toPixelSize(getSize(), scalesWithGuiSize());

        float thicknessX = rimThickness * Window.getWidth() / (size.x * Window.getAspectRatio());
        float thicknessY = rimThickness * Window.getHeight() / size.y;

        blackBox.setSizeToParent(1.0f - 2 * thicknessX, 1.0f - 2 * thicknessY);
        blackBox.setOffsetToParent(thicknessX, thicknessY);

        float blackBoxSize = blackBox.getSize().x;
        nameElement.setOffsetToParent(Math.max(0.05f, 0.5f - nameElement.getLength() * 0.5f / blackBoxSize), 0.5f);
        textElement.setOffsetToParent(Math.max(0.05f, 0.5f - textElement.getLength() * 0.5f / blackBoxSize), 0.5f);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new TextFieldInput(this));
    }

    private Clickable getAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            this.setOnTop();
        };
    }

    private String text = "";
    private final Runnable onTextChange;
    private final TextElement textElement;
    private final TextElement nameElement;
    private final UiElement blackBox;
}
