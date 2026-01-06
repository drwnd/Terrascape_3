package core.renderables;

import core.assets.CoreTextures;
import core.rendering_api.Window;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.OptionSetting;
import core.settings.optionSettings.FontOption;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends UiButton {

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
        nameElement.setColor(Color.GRAY);

        cursorIndicator = new UiElement(new Vector2f(), new Vector2f(), CoreTextures.CURSOR_INDICATOR);
        cursorIndicator.setAllowFocusScaling(false);

        blackBox = new UiElement(new Vector2f(), new Vector2f(), CoreTextures.OVERLAY);
        blackBox.addRenderable(cursorIndicator);
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

    public void setCenterText(boolean centerText) {
        this.centerText = centerText;
    }


    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        textElement.setText(text);

        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0F;
        float rimThickness = FloatSetting.RIM_THICKNESS.value() * guiSize * getRimThicknessMultiplier();
        size = Window.toPixelSize(getSize(), scalesWithGuiSize());

        float thicknessX = rimThickness * Window.getWidth() / (size.x * Window.getAspectRatio());
        float thicknessY = rimThickness * Window.getHeight() / size.y;

        blackBox.setSizeToParent(1.0F - 2 * thicknessX, 1.0F - 2 * thicknessY);
        blackBox.setOffsetToParent(thicknessX, thicknessY);

        float blackBoxSize = blackBox.getSize().x;
        if (centerText) {
            nameElement.setOffsetToParent(Math.max(0.05F, 0.5F - nameElement.getLength() * 0.5F / blackBoxSize), 0.5F);
            textElement.setOffsetToParent(Math.max(0.05F, 0.5F - textElement.getLength() * 0.5F / blackBoxSize), 0.5F);
        } else {
            nameElement.setOffsetToParent(0.05F, 0.5F);
            textElement.setOffsetToParent(0.05F, 0.5F);
        }

        boolean indicatorVisible = !text.isEmpty() && Window.getInput() instanceof TextFieldInput input && input.field == this;
        cursorIndicator.setVisible(indicatorVisible);
        if (indicatorVisible) {
            int cursorIndex = ((TextFieldInput) Window.getInput()).getCursorIndex();
            Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();

            float cursorIndexOffset = TextShader.getTextLength(text.substring(0, cursorIndex), defaultTextSize.x, scalesWithGuiSize());
            cursorIndexOffset /= getSizeToParent().x * blackBox.getSizeToParent().x;
            if (allowsFocusScaling() && isFocused()) cursorIndexOffset /= getScalingFactor();
            cursorIndexOffset += textElement.getOffsetToParent().x;

            float textSize = FloatSetting.TEXT_SIZE.value();
            float charHeight = Window.getHeight() * defaultTextSize.y * textSize;
            float indicatorHeight = textSize * charHeight / (blackBox.getSize().y * Window.getHeight());
            float indicatorWidth = TextShader.getTextLength("|", defaultTextSize.x, scalesWithGuiSize()) * 0.5F / blackBoxSize;

            cursorIndicator.setOffsetToParent(cursorIndexOffset, 0.5F - indicatorHeight * 0.5F);
            cursorIndicator.setSizeToParent(indicatorWidth, indicatorHeight);
        }
    }

    @Override
    public void setOnTop() {
        Window.setInput(new TextFieldInput(this));
    }

    private Clickable getAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            this.setOnTop();
        };
    }

    private String text = "";
    private final Runnable onTextChange;
    private final TextElement textElement;
    private final TextElement nameElement;
    private final UiElement blackBox, cursorIndicator;
    private boolean centerText = true;
}
