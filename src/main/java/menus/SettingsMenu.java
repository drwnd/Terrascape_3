package menus;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import player.rendering.DebugScreenLine;
import renderables.*;
import rendering_api.Window;
import settings.FloatSetting;
import settings.KeySetting;
import settings.OptionSetting;
import settings.ToggleSetting;

import java.util.ArrayList;

public final class SettingsMenu extends UiBackgroundElement {

    public SettingsMenu() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));

        UiButton backButton = new UiButton(new Vector2f(0.25f, 0.1f), new Vector2f(0.05f, 0.85f), getBackButtonAction());
        TextElement text = new TextElement(new Vector2f(0.15f, 0.5f), "Back");
        backButton.addRenderable(text);

        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.05f, 0.5f);

        UiButton everythingSectionButton = new UiButton(sizeToParent, new Vector2f(0.35f, 0.85f), sectionButtonAction(this::createEverythingSection));
        text = new TextElement(offsetToParent, "Everything");
        everythingSectionButton.addRenderable(text);

        UiButton controlsSection = new UiButton(sizeToParent, new Vector2f(0.35f, 0.7f), sectionButtonAction(this::createControlsSection));
        text = new TextElement(offsetToParent, "Controls");
        controlsSection.addRenderable(text);

        UiButton renderingSection = new UiButton(sizeToParent, new Vector2f(0.35f, 0.55f), sectionButtonAction(this::createRenderingSection));
        text = new TextElement(offsetToParent, "Rendering");
        renderingSection.addRenderable(text);

        UiButton uiSection = new UiButton(sizeToParent, new Vector2f(0.35f, 0.4f), sectionButtonAction(this::createUiSection));
        text = new TextElement(offsetToParent, "Ui Customization");
        uiSection.addRenderable(text);

        UiButton soundSection = new UiButton(sizeToParent, new Vector2f(0.35f, 0.25f), sectionButtonAction(this::createSoundSection));
        text = new TextElement(offsetToParent, "Sound");
        soundSection.addRenderable(text);

        UiButton debugSection = new UiButton(sizeToParent, new Vector2f(0.35f, 0.1f), sectionButtonAction(this::createDebugSection));
        text = new TextElement(offsetToParent, "Debug");
        debugSection.addRenderable(text);

        UiButton debugScreenSection = new UiButton(sizeToParent, new Vector2f(0.35f, -0.05f), sectionButtonAction(this::createDebugScreenSection));
        text = new TextElement(offsetToParent, "Debug Screen");
        debugScreenSection.addRenderable(text);

        addRenderable(backButton);

        addRenderable(everythingSectionButton);
        addRenderable(controlsSection);
        addRenderable(renderingSection);
        addRenderable(uiSection);
        addRenderable(soundSection);
        addRenderable(debugSection);
        addRenderable(debugScreenSection);

        sectionButtons.add(everythingSectionButton);
        sectionButtons.add(controlsSection);
        sectionButtons.add(renderingSection);
        sectionButtons.add(uiSection);
        sectionButtons.add(soundSection);
        sectionButtons.add(debugSection);
        sectionButtons.add(debugScreenSection);
    }

    public void scrollSectionButtons(float scroll) {
        Vector2f offset = new Vector2f(0, scroll);

        for (Renderable renderable : sectionButtons) renderable.move(offset);
    }

    private SettingsRenderable createEverythingSection() {
        SettingsRenderable section = new SettingsRenderable();

        for (FloatSetting setting : FloatSetting.values()) section.addSlider(setting);
        for (ToggleSetting setting : ToggleSetting.values()) section.addToggle(setting);
        for (KeySetting setting : KeySetting.values()) section.addKeySelector(setting);
        for (OptionSetting setting : OptionSetting.values()) section.addOption(setting);

        return section;
    }

    private SettingsRenderable createControlsSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addToggle(ToggleSetting.SCROLL_HOT_BAR);
        section.addToggle(ToggleSetting.RAW_MOUSE_INPUT);

        section.addSlider(FloatSetting.SENSITIVITY);

        section.addKeySelector(KeySetting.MOVE_FORWARD);
        section.addKeySelector(KeySetting.MOVE_BACK);
        section.addKeySelector(KeySetting.MOVE_RIGHT);
        section.addKeySelector(KeySetting.MOVE_LEFT);
        section.addKeySelector(KeySetting.JUMP);
        section.addKeySelector(KeySetting.SPRINT);
        section.addKeySelector(KeySetting.SNEAK);
        section.addKeySelector(KeySetting.CRAWL);
        section.addKeySelector(KeySetting.FLY_FAST);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_1);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_2);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_3);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_4);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_5);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_6);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_7);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_8);
        section.addKeySelector(KeySetting.HOT_BAR_SLOT_9);
        section.addKeySelector(KeySetting.DESTROY);
        section.addKeySelector(KeySetting.USE);
        section.addKeySelector(KeySetting.PICK_BLOCK);
        section.addKeySelector(KeySetting.INVENTORY);
        section.addKeySelector(KeySetting.ZOOM);
        section.addKeySelector(KeySetting.INCREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySetting.DECREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySetting.DROP);
        section.addKeySelector(KeySetting.RESIZE_WINDOW);

        return section;
    }

    private SettingsRenderable createRenderingSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSetting.FOV);
        section.addSlider(FloatSetting.CROSSHAIR_SIZE);
        section.addSlider(FloatSetting.HOTBAR_SIZE);

        section.addToggle(ToggleSetting.DO_SHADOW_MAPPING);

        return section;
    }

    private SettingsRenderable createUiSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSetting.GUI_SIZE);
        section.addSlider(FloatSetting.TEXT_SIZE);
        section.addSlider(FloatSetting.RIM_THICKNESS);
        section.addSlider(FloatSetting.HOTBAR_INDICATOR_SCALER);

        return section;
    }

    private SettingsRenderable createSoundSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSetting.MASTER_AUDIO);
        section.addSlider(FloatSetting.FOOTSTEPS_AUDIO);
        section.addSlider(FloatSetting.PLACE_AUDIO);
        section.addSlider(FloatSetting.DIG_AUDIO);
        section.addSlider(FloatSetting.INVENTORY_AUDIO);
        section.addSlider(FloatSetting.MISCELLANEOUS_AUDIO);

        return section;
    }

    private SettingsRenderable createDebugSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addKeySelector(KeySetting.DEBUG_MENU);
        section.addKeySelector(KeySetting.NO_CLIP);
        section.addKeySelector(KeySetting.RESIZE_WINDOW);
        section.addKeySelector(KeySetting.RELOAD_SETTINGS);
        section.addKeySelector(KeySetting.RELOAD_ASSETS);
        section.addKeySelector(KeySetting.TOGGLE_FLYING_FOLLOWING_MOVEMENT_STATE);

        section.addToggle(ToggleSetting.X_RAY);
        section.addToggle(ToggleSetting.V_SYNC);

        section.addSlider(FloatSetting.REACH);
        section.addSlider(FloatSetting.BREAK_PLACE_INTERVALL);

        return section;
    }

    private SettingsRenderable createDebugScreenSection() {
        SettingsRenderable section = new SettingsRenderable();

        for (DebugScreenLine debugLine : DebugScreenLine.getDebugLines()) section.addDebugLineSetting(debugLine);

        return section;
    }

    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0f : input.getScroll();
        input = new SettingsMenuInput(this);
        input.setScroll(scroll);
        Window.setInput(input);
    }

    private Clickable getBackButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.popRenderable();
        };
    }

    private Clickable sectionButtonAction(SectionCreator sectionCreator) {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.pushRenderable(sectionCreator.getSection());
        };
    }

    private SettingsMenuInput input;
    private final ArrayList<UiButton> sectionButtons = new ArrayList<>();

    private interface SectionCreator {
        SettingsRenderable getSection();
    }
}
