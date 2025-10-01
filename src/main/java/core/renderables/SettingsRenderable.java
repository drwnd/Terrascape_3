package core.renderables;

import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.settings.OptionSetting;
import core.settings.ToggleSetting;
import core.rendering_api.Window;
import core.settings.*;
import core.utils.Message;
import core.utils.StringGetter;
import core.languages.UiMessage;

import game.player.rendering.DebugScreenLine;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public final class SettingsRenderable extends UiBackgroundElement {

    public SettingsRenderable() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));
        input = new SettingsRenderableInput(this);
        Vector2f sizeToParent = new Vector2f(0.1f, 0.1f);

        UiButton backButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.85f), getBackButtonAction());
        TextElement text = new TextElement(new Vector2f(0.15f, 0.5f), UiMessage.BACK);
        backButton.addRenderable(text);

        UiButton applyChangesButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.7f), getApplyChangesButtonAction());
        text = new TextElement(new Vector2f(0.15f, 0.5f), UiMessage.APPLY_SETTINGS);
        applyChangesButton.addRenderable(text);

        UiButton resetButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.55f), getResetSettingsButtonAction());
        text = new TextElement(new Vector2f(0.15f, 0.5f), UiMessage.RESET_ALL_SETTINGS);
        resetButton.addRenderable(text);

        addRenderable(backButton);
        addRenderable(applyChangesButton);
        addRenderable(resetButton);
    }

    public void scrollSettingButtons(float scroll) {
        Vector2f offset = new Vector2f(0, scroll);

        for (Slider slider : sliders) slider.move(offset);
        for (KeySelector keySelector : keySelectors) keySelector.move(offset);
        for (Toggle toggle : toggles) toggle.move(offset);
        for (OptionToggle option : options) option.move(offset);

        for (UiButton resetButton : resetButtons) resetButton.move(offset);
        for (Renderable renderable : movingRenderables) renderable.move(offset);
    }

    public void setSelectedSlider(Slider slider) {
        this.selectedSlider = slider;
    }

    public void cancelSelection() {
        if (selectedSlider != null) {
            selectedSlider = null;
            return;
        }
        Window.popRenderable();
    }

    public void addSlider(FloatSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - 0.15f * settingsCount);

        Slider slider = new Slider(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(slider);
        sliders.add(slider);

        createResetButton(settingsCount).setAction((Vector2i cursorPos, int button, int action) -> {
            if (action == GLFW.GLFW_PRESS) slider.setToDefault();
        });
    }

    public void addKeySelector(KeySetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - 0.15f * settingsCount);

        KeySelector keySelector = new KeySelector(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(keySelector);
        keySelectors.add(keySelector);

        createResetButton(settingsCount).setAction((Vector2i cursorPos, int button, int action) -> {
            if (action == GLFW.GLFW_PRESS) keySelector.setToDefault();
        });
    }

    public void addToggle(ToggleSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - 0.15f * settingsCount);

        Toggle toggle = new Toggle(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(toggle);
        toggles.add(toggle);

        createResetButton(settingsCount).setAction((Vector2i cursorPos, int button, int action) -> {
            if (action == GLFW.GLFW_PRESS) toggle.setToDefault();
        });
    }

    public void addOption(OptionSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - 0.15f * settingsCount);

        OptionToggle option = new OptionToggle(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(option);
        options.add(option);

        createResetButton(settingsCount).setAction((Vector2i cursorPos, int button, int action) -> {
            if (action == GLFW.GLFW_PRESS) option.setToDefault();
        });
    }

    public void addDebugLineSetting(DebugScreenLine debugLine) {
        settingsCount++;

        Vector2f sizeToParent = new Vector2f(0.15f, 0.1f);
        float yOffset = 1.0f - 0.15f * settingsCount;

        TextElement nameDisplay = new TextElement(new Vector2f(0.225f, 0), new Vector2f(0.375f, yOffset + 0.05f), new Message(debugLine.name()));
        nameDisplay.setAllowFocusScaling(false);
        OptionToggle colorOption = new OptionToggle(sizeToParent, new Vector2f(0.6f, yOffset), debugLine.color(), null);
        OptionToggle visibilityOption = new OptionToggle(sizeToParent, new Vector2f(0.8f, yOffset), debugLine.visibility(), null);

        addRenderable(nameDisplay);
        addRenderable(colorOption);
        addRenderable(visibilityOption);

        movingRenderables.add(nameDisplay);
        options.add(colorOption);
        options.add(visibilityOption);

        createResetButton(settingsCount).setAction((Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            colorOption.setToDefault();
            visibilityOption.setToDefault();
        });
    }


    private UiButton createResetButton(int counter) {
        Vector2f sizeToParent = new Vector2f(0.1f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.225f, 1.0f - 0.15f * counter);
        UiButton resetButton = new UiButton(sizeToParent, offsetToParent);

        TextElement text = new TextElement(new Vector2f(0.15f, 0.5f), UiMessage.RESET_SETTING);
        resetButton.addRenderable(text);

        addRenderable(resetButton);
        resetButtons.add(resetButton);
        return resetButton;
    }

    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0f : input.getScroll();
        input = new SettingsRenderableInput(this);
        input.setScroll(scroll);
        Window.setInput(input);
        selectedSlider = null;
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        if (selectedSlider != null)
            selectedSlider.dragOver(pixelCoordinate);
        else super.dragOver(pixelCoordinate);
    }

    @Override
    public void clickOn(Vector2i pixelCoordinate, int mouseButton, int action) {
        boolean buttonFound = false;
        for (Renderable renderable : getChildren())
            if (renderable.isVisible() && renderable.containsPixelCoordinate(pixelCoordinate)) {
                renderable.clickOn(pixelCoordinate, mouseButton, action);
                buttonFound = true;
                break;
            }

        if (!buttonFound) selectedSlider = null;
    }

    private Clickable getApplyChangesButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;

            for (Slider slider : sliders) Settings.update(slider.getSetting(), slider.getValue());
            for (KeySelector keySelector : keySelectors) Settings.update(keySelector.getSetting(), keySelector.getValue());
            for (Toggle toggle : toggles) Settings.update(toggle.getSetting(), toggle.getValue());
            for (OptionToggle option : options) Settings.update(option.getSetting(), option.getValue());

            Settings.writeToFile();
            Window.popRenderable();
        };
    }

    private Clickable getResetSettingsButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            for (UiButton resetButton : resetButtons) resetButton.clickOn(pixelCoordinate, button, action);
        };
    }

    private Clickable getBackButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;

            for (Slider slider : sliders) slider.matchSetting();
            for (KeySelector keySelector : keySelectors) keySelector.matchSetting();
            for (Toggle toggle : toggles) toggle.matchSetting();
            for (OptionToggle option : options) option.matchSetting();

            Window.popRenderable();
        };
    }

    private int settingsCount = 0;
    private Slider selectedSlider;
    private SettingsRenderableInput input;

    private final ArrayList<Slider> sliders = new ArrayList<>();
    private final ArrayList<KeySelector> keySelectors = new ArrayList<>();
    private final ArrayList<Toggle> toggles = new ArrayList<>();
    private final ArrayList<OptionToggle> options = new ArrayList<>();

    private final ArrayList<UiButton> resetButtons = new ArrayList<>();
    private final ArrayList<Renderable> movingRenderables = new ArrayList<>();
}
