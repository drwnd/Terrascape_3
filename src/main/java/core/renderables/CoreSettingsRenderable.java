package core.renderables;

import core.rendering_api.Window;
import core.settings.*;
import core.utils.StringGetter;
import core.languages.UiMessage;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class CoreSettingsRenderable extends UiBackgroundElement {

    public CoreSettingsRenderable() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        input = new SettingsRenderableInput(this);
        Vector2f sizeToParent = new Vector2f(0.1F, 0.1F);

        UiButton backButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), getBackButtonAction());
        TextElement text = new TextElement(new Vector2f(0.15F, 0.5F), UiMessage.BACK);
        backButton.addRenderable(text);

        UiButton applyChangesButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.7f), getApplyChangesButtonAction());
        text = new TextElement(new Vector2f(0.15F, 0.5F), UiMessage.APPLY_SETTINGS);
        applyChangesButton.addRenderable(text);

        UiButton resetButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.55F), getResetSettingsButtonAction());
        text = new TextElement(new Vector2f(0.15F, 0.5F), UiMessage.RESET_ALL_SETTINGS);
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
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * settingsCount);

        Slider slider = new Slider(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(slider);
        sliders.add(slider);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action == GLFW_PRESS) slider.setToDefault();
        });
    }

    public void addKeySelector(KeySetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * settingsCount);

        KeySelector keySelector = new KeySelector(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(keySelector);
        keySelectors.add(keySelector);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action == GLFW_PRESS) keySelector.setToDefault();
        });
    }

    public void addToggle(ToggleSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.2875F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * settingsCount);

        Toggle toggle = new Toggle(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(toggle);
        toggles.add(toggle);

        offsetToParent = new Vector2f(0.6625F, 1.0F - 0.15F * settingsCount);
        sizeToParent = new Vector2f(sizeToParent);
        KeySelector keySelector = new KeySelector(sizeToParent, offsetToParent, setting, UiMessage.KEYBIND);
        addRenderable(keySelector);
        keySelectors.add(keySelector);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action == GLFW_PRESS) toggle.setToDefault();
            if (action == GLFW_PRESS) keySelector.setToDefault();
        });
    }

    public void addOption(OptionSetting setting, StringGetter settingName) {
        settingsCount++;
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * settingsCount);

        OptionToggle option = new OptionToggle(sizeToParent, offsetToParent, setting, settingName);
        addRenderable(option);
        options.add(option);

        createResetButton(settingsCount).setAction((Vector2i _, int _, int action) -> {
            if (action == GLFW_PRESS) option.setToDefault();
        });
    }


    protected UiButton createResetButton(int counter) {
        Vector2f sizeToParent = new Vector2f(0.1F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.225F, 1.0F - 0.15F * counter);
        UiButton resetButton = new UiButton(sizeToParent, offsetToParent);

        TextElement text = new TextElement(new Vector2f(0.15F, 0.5F), UiMessage.RESET_SETTING);
        resetButton.addRenderable(text);

        addRenderable(resetButton);
        resetButtons.add(resetButton);
        return resetButton;
    }

    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0F : input.getScroll();
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
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;

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
            if (action != GLFW_PRESS) return;
            for (UiButton resetButton : resetButtons) resetButton.clickOn(pixelCoordinate, button, action);
        };
    }

    private Clickable getBackButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;

            for (Slider slider : sliders) slider.matchSetting();
            for (KeySelector keySelector : keySelectors) keySelector.matchSetting();
            for (Toggle toggle : toggles) toggle.matchSetting();
            for (OptionToggle option : options) option.matchSetting();

            Window.popRenderable();
        };
    }

    protected int settingsCount = 0;
    private Slider selectedSlider;
    private SettingsRenderableInput input;

    protected final ArrayList<Slider> sliders = new ArrayList<>();
    protected final ArrayList<KeySelector> keySelectors = new ArrayList<>();
    protected final ArrayList<Toggle> toggles = new ArrayList<>();
    protected final ArrayList<OptionToggle> options = new ArrayList<>();

    private final ArrayList<UiButton> resetButtons = new ArrayList<>();
    protected final ArrayList<Renderable> movingRenderables = new ArrayList<>();
}
