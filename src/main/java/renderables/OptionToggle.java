package renderables;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import settings.OptionSetting;
import settings.optionSettings.ColorOption;
import settings.optionSettings.Option;

public class OptionToggle extends UiButton {
    public OptionToggle(Vector2f sizeToParent, Vector2f offsetToParent, OptionSetting setting) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());

        this.setting = setting;

        textElement = new TextElement(new Vector2f(0.05f, 0.5f));
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
        textElement.setText(value.name());
        if (value instanceof ColorOption) textElement.setColor(((ColorOption) value).getColor());
    }

    private Clickable getAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) setValue(value.next());
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) setValue(value.previous());
        };
    }

    private Option value;
    private final OptionSetting setting;
    private final TextElement textElement;
}
