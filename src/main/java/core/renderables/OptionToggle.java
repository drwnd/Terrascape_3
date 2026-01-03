package core.renderables;

import core.settings.OptionSetting;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.Option;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class OptionToggle extends UiButton {
    public OptionToggle(Vector2f sizeToParent, Vector2f offsetToParent, OptionSetting setting, StringGetter settingName) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());

        this.setting = setting;
        this.settingName = settingName;

        textElement = new TextElement(new Vector2f(0.05F, 0.5F));
        addRenderable(textElement);

        matchSetting();
    }

    public void setToDefault() {
        setValue(setting.defaultValue());
    }

    public OptionSetting getSetting() {
        return setting;
    }

    public Option getValue() {
        return value;
    }

    public void matchSetting() {
        setValue(setting.value());
    }


    private void setValue(Option value) {
        this.value = value;
        if (settingName != null) textElement.setText(settingName.get() + " " + value.name());
        else textElement.setText(value.name());
        if (value instanceof ColorOption) textElement.setColor(((ColorOption) value).getColor());
    }

    private Clickable getAction() {
        return (Vector2i _, int button, int action) -> {
            if (action != GLFW_PRESS) return;
            if (button == GLFW_MOUSE_BUTTON_LEFT) setValue(value.next());
            if (button == GLFW_MOUSE_BUTTON_RIGHT) setValue(value.previous());
        };
    }

    private final StringGetter settingName;
    private Option value;
    private final OptionSetting setting;
    private final TextElement textElement;
}
