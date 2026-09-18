package core.renderables;

import core.assets.CoreTextures;
import core.settings.NumberSetting;
import core.utils.MainThread;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class Slider<T extends Number> extends UiButton {

    @MainThread
    public Slider(Vector2f sizeToParent, Vector2f offsetToParent, NumberSetting<T> setting, StringGetter settingName, boolean updateImmediately) {
        super(sizeToParent, offsetToParent);
        setAction(this::setValueOnClick);
        this.setting = setting;
        this.settingName = settingName;
        this.updateImmediately = updateImmediately;

        slider = new UiBackgroundElement(new Vector2f(0.05F, 1.0F), new Vector2f(0.0F, 0.0F));
        slider.setTextureIdentifier(CoreTextures.SLIDER_ICON);
        textElement = new TextElement(new Vector2f(0.05F, 0.5F), settingName);

        addRenderable(slider);
        addRenderable(textElement);

        matchSetting();
    }

    @MainThread
    public void setToDefault() {
        setValue(setting.defaultValueGeneric());
    }

    public NumberSetting<T> getSetting() {
        return setting;
    }

    public Number getValue() {
        return value;
    }

    @MainThread
    public void setValue(T value) {
        this.value = value;
        textElement.setText("%s %s".formatted(settingName.get(), value));
        slider.setOffsetToParent(setting.fractionFromValue(value) - slider.getSizeToParent().x * 0.5F, 0.0F);
        if (updateImmediately) setting.setValue(value);
    }

    @Override
    @MainThread
    public void dragOver(Vector2i pixelCoordinate) {
        setValueOnClick(pixelCoordinate, GLFW_MOUSE_BUTTON_LEFT, GLFW_HOVERED);
    }

    @MainThread
    public void matchSetting() {
        setValue(setting.valueGeneric());
    }


    @MainThread
    private ButtonResult setValueOnClick(Vector2i cursorPos, int button, int action) {
        DraggableInfo info = getOwnDraggableInfo(action);
        if (info == null) return ButtonResult.IGNORE;

        float fraction = (cursorPos.x - info.position().x) / info.size().x;
        fraction = Math.clamp(fraction, 0.0F, 1.0F);
        setValue(setting.valueFromFraction(fraction));
        return ButtonResult.SUCCESS;
    }

    private final boolean updateImmediately;
    private final NumberSetting<T> setting;
    private final UiBackgroundElement slider;
    private final TextElement textElement;
    private final StringGetter settingName;
    private Number value;
}
