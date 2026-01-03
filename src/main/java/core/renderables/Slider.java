package core.renderables;

import core.rendering_api.Window;
import core.settings.FloatSetting;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class Slider extends UiButton {

    public Slider(Vector2f sizeToParent, Vector2f offsetToParent, FloatSetting setting, StringGetter settingName) {
        super(sizeToParent, offsetToParent);
        setAction(this::action);
        setAllowFocusScaling(false);
        this.setting = setting;
        this.settingName = settingName;

        slider = new UiBackgroundElement(new Vector2f(0.05F, 1.0F), new Vector2f(0.0F, 0.0F));
        textElement = new TextElement(new Vector2f(0.05F, 0.5F), settingName);

        addRenderable(slider);
        addRenderable(textElement);

        matchSetting();
    }

    public void setToDefault() {
        setValue(setting.defaultValue());
    }

    public FloatSetting getSetting() {
        return setting;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
        textElement.setText("%s %s".formatted(settingName.get(), value));
        slider.setOffsetToParent(setting.fractionFromValue(value) - slider.getSizeToParent().x * 0.5F, 0.0F);
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        action(pixelCoordinate, GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
    }

    public void matchSetting() {
        setValue(setting.value());
    }


    private void action(Vector2i cursorPos, int button, int action) {
        if (action != GLFW_PRESS) return;
        Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
        Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());

        float fraction = (cursorPos.x - position.x) / size.x;
        fraction = Math.clamp(fraction, 0.0F, 1.0F);
        setValue(setting.valueFronFraction(fraction));

        if (getParent() instanceof CoreSettingsRenderable)
            ((CoreSettingsRenderable) getParent()).setSelectedSlider(this);
    }

    private final FloatSetting setting;
    private final UiBackgroundElement slider;
    private final TextElement textElement;
    private final StringGetter settingName;
    private float value;
}
