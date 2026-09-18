package core.renderables;

import core.rendering_api.MenuInput;
import core.rendering_api.Window;
import core.settings.*;
import core.utils.MainThread;
import core.utils.StringGetter;
import core.language.CoreUiMessages;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class CoreSettingsRenderable extends UiBackgroundElement {

    @MainThread
    public CoreSettingsRenderable() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        input = new SettingsRenderableInput(this);
        scroller = new SideScroller(new Vector2f(0.05F, 0.95F), new Vector2f(0.95F, 0.025F), input);
        Vector2f sizeToParent = new Vector2f(0.1F, 0.1F);

        UiButton backButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.875F), getBackButtonAction());
        TextElement text = new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.BACK);
        backButton.addRenderable(text);

        UiButton applyChangesButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.725F), getApplyChangesButtonAction());
        text = new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.APPLY_SETTINGS);
        applyChangesButton.addRenderable(text);

        UiButton resetButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.575F), getResetSettingsButtonAction());
        text = new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.RESET_ALL_SETTINGS);
        resetButton.addRenderable(text);

        addRenderable(backButton);
        addRenderable(applyChangesButton);
        addRenderable(resetButton);
        addRenderable(scroller);
    }

    @MainThread
    public void scrollSettingButtons(float scroll) {
        Vector2f offset = new Vector2f(0, scroll);

        for (Slider<? extends Number> slider : sliders) slider.move(offset);
        for (KeySelector keySelector : keySelectors) keySelector.move(offset);
        for (Toggle toggle : toggles) toggle.move(offset);
        for (OptionToggle option : options) option.move(offset);

        for (UiButton resetButton : resetButtons) resetButton.move(offset);
        for (Renderable renderable : movingRenderables) renderable.move(offset);
    }

    public float getMaxScroll() {
        int settingCount = Math.max(Math.max(Math.max(Math.max(Math.max(
                sliders.size(), keySelectors.size()), toggles.size()), options.size()), resetButtons.size()), movingRenderables.size());
        return settingCount * SETTING_DISTANCE - 1 + 0.025F;
    }

    @MainThread
    public static void cancelSelection() {
        Window.popRenderable();
    }


    @MainThread
    public <T extends Number> void addSlider(NumberSetting<T> setting) {
        addSlider(setting, setting);
    }

    @MainThread
    public void addKeySelector(KeySetting setting) {
        addKeySelector(setting, setting);
    }

    @MainThread
    public void addToggle(ToggleSetting setting) {
        addToggle(setting, setting);
    }

    @MainThread
    public void addOption(OptionSetting setting) {
        addOption(setting, setting);
    }


    @MainThread
    public <T extends Number> void addSlider(NumberSetting<T> setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.3125F, 1.0F - SETTING_DISTANCE * settingsCount);

        Slider<T> slider = new Slider<>(sizeToParent, offsetToParent, setting, settingName, false);
        addRenderable(slider);
        sliders.add(slider);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            slider.setToDefault();
            return ButtonResult.SUCCESS;
        });
    }

    @MainThread
    public void addKeySelector(KeySetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.3125F, 1.0F - SETTING_DISTANCE * settingsCount);

        KeySelector keySelector = new KeySelector(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(keySelector);
        keySelectors.add(keySelector);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            keySelector.setToDefault();
            return ButtonResult.SUCCESS;
        });
    }

    @MainThread
    public void addToggle(ToggleSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.2875F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.3125F, 1.0F - SETTING_DISTANCE * settingsCount);

        Toggle toggle = new Toggle(sizeToParent, offsetToParent, setting, settingName, false);
        addRenderable(toggle);
        toggles.add(toggle);

        offsetToParent = new Vector2f(0.625F, 1.0F - SETTING_DISTANCE * settingsCount);
        KeySelector keySelector = new KeySelector(sizeToParent, offsetToParent, setting, CoreUiMessages.KEYBIND);
        addRenderable(keySelector);
        keySelectors.add(keySelector);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            toggle.setToDefault();
            keySelector.setToDefault();
            return ButtonResult.SUCCESS;
        });
    }

    @MainThread
    public void addOption(OptionSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.18333334F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.3125F, 1.0F - SETTING_DISTANCE * settingsCount);

        OptionToggle option = new OptionToggle(sizeToParent, offsetToParent, setting, settingName, false);
        addRenderable(option);
        options.add(option);

        offsetToParent = new Vector2f(0.3125F + sizeToParent.x + 0.025F, 1.0F - SETTING_DISTANCE * settingsCount);
        KeySelector nextKeySelector = new KeySelector(sizeToParent, offsetToParent, setting.nextKeySetting(), CoreUiMessages.NEXT);
        addRenderable(nextKeySelector);
        keySelectors.add(nextKeySelector);

        offsetToParent = new Vector2f(0.3125F + 2 * sizeToParent.x + 0.05F, 1.0F - SETTING_DISTANCE * settingsCount);
        KeySelector previeousKeySelector = new KeySelector(sizeToParent, offsetToParent, setting.previousKeySetting(), CoreUiMessages.PREVIOUS);
        addRenderable(previeousKeySelector);
        keySelectors.add(previeousKeySelector);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            option.setToDefault();
            nextKeySelector.setToDefault();
            previeousKeySelector.setToDefault();
            return ButtonResult.SUCCESS;
        });
    }


    @MainThread
    protected UiButton createResetButton(int counter) {
        Vector2f sizeToParent = new Vector2f(0.1F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.1875F, 1.0F - SETTING_DISTANCE * counter);
        UiButton resetButton = new UiButton(sizeToParent, offsetToParent);

        TextElement text = new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.RESET_SETTING);
        resetButton.addRenderable(text);

        addRenderable(resetButton);
        resetButtons.add(resetButton);
        return resetButton;
    }

    @Override
    @MainThread
    public void setOnTop() {
        float scroll = input == null ? 0.0F : input.getScroll();
        input = new SettingsRenderableInput(this);
        input.setScroll(scroll);
        scroller.setInput(input);
        Window.setInput(input);
    }

    @MainThread
    Clickable getApplyChangesButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            for (Slider<? extends Number> slider : sliders) slider.getSetting().setValue(slider.getValue());
            for (KeySelector keySelector : keySelectors) keySelector.getSetting().setKeybind(keySelector.getValue());
            for (Toggle toggle : toggles) toggle.getSetting().setValue(toggle.getValue());
            for (OptionToggle option : options) option.getSetting().setValue(option.getValue());

            Settings.writeToFile();
            Window.popRenderable();
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    Clickable getResetSettingsButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            for (UiButton resetButton : resetButtons) resetButton.clickOn(pixelCoordinate, button, action);
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    Clickable getBackButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            for (Slider<? extends Number> slider : sliders) slider.matchSetting();
            for (KeySelector keySelector : keySelectors) keySelector.matchSetting();
            for (Toggle toggle : toggles) toggle.matchSetting();
            for (OptionToggle option : options) option.matchSetting();

            Window.popRenderable();
            return ButtonResult.SUCCESS;
        };
    }

    protected int settingsCount = 0;
    protected SettingsRenderableInput input;
    protected SideScroller scroller;

    protected final ArrayList<Slider<? extends Number>> sliders = new ArrayList<>();
    protected final ArrayList<KeySelector> keySelectors = new ArrayList<>();
    protected final ArrayList<Toggle> toggles = new ArrayList<>();
    protected final ArrayList<OptionToggle> options = new ArrayList<>();

    private final ArrayList<UiButton> resetButtons = new ArrayList<>();
    protected final ArrayList<Renderable> movingRenderables = new ArrayList<>();

    protected static final float SETTING_DISTANCE = 0.125F;

    protected static final class SettingsRenderableInput extends MenuInput<CoreSettingsRenderable> {

        @MainThread
        public SettingsRenderableInput(CoreSettingsRenderable menu) {
            super(menu, menu::scrollSettingButtons, menu::getMaxScroll);
        }

        @Override
        @MainThread
        public void keyCallback(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) CoreSettingsRenderable.cancelSelection();
        }
    }
}
