package core.renderables;

import core.rendering_api.Window;
import core.settings.NumberSetting;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class Slider<T extends Number> extends UiButton {

    public Slider(Vector2f sizeToParent, Vector2f offsetToParent, NumberSetting<T> setting, StringGetter settingName, boolean updateImmediately) {
        super(sizeToParent, offsetToParent);
        setAction(this::action);
        setDoAutoFocusScaling(false);
        this.setting = setting;
        this.settingName = settingName;
        this.updateImmediately = updateImmediately;

        slider = new UiBackgroundElement(new Vector2f(0.05F, 1.0F), new Vector2f(0.0F, 0.0F));
        textElement = new TextElement(new Vector2f(0.05F, 0.5F), settingName);

        addRenderable(slider);
        addRenderable(textElement);

        matchSetting();
    }

    public void setToDefault() {
        setValue(setting.defaultValueGeneric());
    }

    public NumberSetting<T> getSetting() {
        return setting;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        textElement.setText("%s %s".formatted(settingName.get(), value));
        slider.setOffsetToParent(setting.fractionFromValue(value) - slider.getSizeToParent().x * 0.5F, 0.0F);
        if (updateImmediately) setting.setValue(value);
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        action(pixelCoordinate, GLFW_MOUSE_BUTTON_LEFT, GLFW_HOVERED);
    }

    public void matchSetting() {
        setValue(setting.valueGeneric());
    }


    private void action(Vector2i cursorPos, int button, int action) {
        if (action == GLFW_HOVERED && selected != this) return;
        if (action == GLFW_PRESS) selected = this;
        if (action == GLFW_RELEASE)
            if (selected == this) selected = null;
            else return;
        Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
        Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());

        float fraction = (cursorPos.x - position.x) / size.x;
        fraction = Math.clamp(fraction, 0.0F, 1.0F);
        setValue(setting.valueFromFraction(fraction));
    }

    private final boolean updateImmediately;
    private final NumberSetting<T> setting;
    private final UiBackgroundElement slider;
    private final TextElement textElement;
    private final StringGetter settingName;
    private Number value;

    private static Slider<?> selected = null;
}
