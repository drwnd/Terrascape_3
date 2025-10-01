package game.menus;

import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.settings.OptionSetting;
import core.settings.ToggleSetting;
import core.utils.StringGetter;
import core.languages.UiMessage;
import core.renderables.*;
import core.rendering_api.Window;

import game.player.rendering.DebugScreenLine;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public final class SettingsMenu extends UiBackgroundElement {

    public SettingsMenu() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));

        UiButton backButton = new UiButton(new Vector2f(0.25f, 0.1f), new Vector2f(0.05f, 0.85f), getBackButtonAction());
        backButton.addRenderable(new TextElement(new Vector2f(0.15f, 0.5f), UiMessage.BACK));
        addRenderable(backButton);

        int index = 0;
        addSection(++index, this::createControlsSection, UiMessage.CONTROLS_SECTION);
        addSection(++index, this::createRenderingSection, UiMessage.RENDERING_SECTION);
        addSection(++index, this::createUiSection, UiMessage.UI_CUSTOMIZATION_SECTION);
        addSection(++index, this::createSoundSection, UiMessage.SOUND_SECTION);
        addSection(++index, this::createDebugSection, UiMessage.DEBUG_SECTION);
        addSection(++index, this::createDebugScreenSection, UiMessage.DEBUG_SCREEN_SECTION);
    }

    public void scrollSectionButtons(float scroll) {
        Vector2f offset = new Vector2f(0, scroll);

        for (Renderable renderable : sectionButtons) renderable.move(offset);
    }

    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0f : input.getScroll();
        input = new SettingsMenuInput(this);
        input.setScroll(scroll);
        Window.setInput(input);
    }


    private SettingsRenderable createControlsSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addToggle(ToggleSetting.SCROLL_HOTBAR, UiMessage.SCROLL_HOTBAR);
        section.addToggle(ToggleSetting.RAW_MOUSE_INPUT, UiMessage.RAW_MOUSE_INPUT);

        section.addSlider(FloatSetting.SENSITIVITY, UiMessage.SENSITIVITY);

        section.addKeySelector(KeySetting.MOVE_FORWARD, UiMessage.MOVE_FORWARD);
        section.addKeySelector(KeySetting.MOVE_BACK, UiMessage.MOVE_BACK);
        section.addKeySelector(KeySetting.MOVE_RIGHT, UiMessage.MOVE_RIGHT);
        section.addKeySelector(KeySetting.MOVE_LEFT, UiMessage.MOVE_LEFT);
        section.addKeySelector(KeySetting.JUMP, UiMessage.JUMP);
        section.addKeySelector(KeySetting.SPRINT, UiMessage.SPRINT);
        section.addKeySelector(KeySetting.SNEAK, UiMessage.SNEAK);
        section.addKeySelector(KeySetting.CRAWL, UiMessage.CRAWL);
        section.addKeySelector(KeySetting.FLY_FAST, UiMessage.FLY_FAST);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_1, UiMessage.HOTBAR_SLOT_1);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_2, UiMessage.HOTBAR_SLOT_2);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_3, UiMessage.HOTBAR_SLOT_3);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_4, UiMessage.HOTBAR_SLOT_4);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_5, UiMessage.HOTBAR_SLOT_5);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_6, UiMessage.HOTBAR_SLOT_6);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_7, UiMessage.HOTBAR_SLOT_7);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_8, UiMessage.HOTBAR_SLOT_8);
        section.addKeySelector(KeySetting.HOTBAR_SLOT_9, UiMessage.HOTBAR_SLOT_9);
        section.addKeySelector(KeySetting.DESTROY, UiMessage.DESTROY);
        section.addKeySelector(KeySetting.USE, UiMessage.USE);
        section.addKeySelector(KeySetting.PICK_BLOCK, UiMessage.PICK_BLOCK);
        section.addKeySelector(KeySetting.INVENTORY, UiMessage.INVENTORY);
        section.addKeySelector(KeySetting.ZOOM, UiMessage.ZOOM);
        section.addKeySelector(KeySetting.INCREASE_BREAK_PLACE_SIZE, UiMessage.INCREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySetting.DECREASE_BREAK_PLACE_SIZE, UiMessage.DECREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySetting.DROP, UiMessage.DROP);
        section.addKeySelector(KeySetting.RESIZE_WINDOW, UiMessage.RESIZE_WINDOW);

        return section;
    }

    private SettingsRenderable createRenderingSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSetting.FOV, UiMessage.FOV);
        section.addSlider(FloatSetting.CROSSHAIR_SIZE, UiMessage.CROSSHAIR_SIZE);
        section.addSlider(FloatSetting.HOTBAR_SIZE, UiMessage.HOTBAR_SIZE);

        section.addToggle(ToggleSetting.DO_SHADOW_MAPPING, UiMessage.DO_SHADOW_MAPPING);

        return section;
    }

    private SettingsRenderable createUiSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addOption(OptionSetting.LANGUAGE, UiMessage.LANGUAGE);
        section.addOption(OptionSetting.FONT, UiMessage.FONT);

        section.addSlider(FloatSetting.GUI_SIZE, UiMessage.GUI_SIZE);
        section.addSlider(FloatSetting.TEXT_SIZE, UiMessage.TEXT_SIZE);
        section.addSlider(FloatSetting.RIM_THICKNESS, UiMessage.RIM_THICKNESS);
        section.addSlider(FloatSetting.HOTBAR_INDICATOR_SCALER, UiMessage.HOTBAR_INDICATOR_SCALER);
        section.addSlider(FloatSetting.PAUSE_MENU_BACKGROUND_BLUR, UiMessage.PAUSE_MENU_BACKGROUND_BLUR);
        section.addSlider(FloatSetting.INVENTORY_ITEM_SIZE, UiMessage.INVENTORY_ITEM_SIZE);
        section.addSlider(FloatSetting.INVENTORY_ITEMS_PER_ROW, UiMessage.INVENTORY_ITEMS_PER_ROW);
        section.addSlider(FloatSetting.INVENTORY_ITEM_SCALING, UiMessage.INVENTORY_ITEM_SCALING);

        return section;
    }

    private SettingsRenderable createSoundSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSetting.MASTER_AUDIO, UiMessage.MASTER_AUDIO);
        section.addSlider(FloatSetting.FOOTSTEPS_AUDIO, UiMessage.FOOTSTEPS_AUDIO);
        section.addSlider(FloatSetting.PLACE_AUDIO, UiMessage.PLACE_AUDIO);
        section.addSlider(FloatSetting.DIG_AUDIO, UiMessage.DIG_AUDIO);
        section.addSlider(FloatSetting.INVENTORY_AUDIO, UiMessage.INVENTORY_AUDIO);
        section.addSlider(FloatSetting.MISCELLANEOUS_AUDIO, UiMessage.MISCELLANEOUS_AUDIO);

        return section;
    }

    private SettingsRenderable createDebugSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addKeySelector(KeySetting.DEBUG_MENU, UiMessage.DEBUG_MENU);
        section.addKeySelector(KeySetting.NO_CLIP, UiMessage.NO_CLIP);
        section.addKeySelector(KeySetting.RESIZE_WINDOW, UiMessage.RESIZE_WINDOW);
        section.addKeySelector(KeySetting.RELOAD_SETTINGS, UiMessage.RELOAD_SETTINGS);
        section.addKeySelector(KeySetting.RELOAD_ASSETS, UiMessage.RELOAD_ASSETS);
        section.addKeySelector(KeySetting.TOGGLE_FLYING_FOLLOWING_MOVEMENT_STATE, UiMessage.TOGGLE_FLYING_FOLLOWING_MOVEMENT_STATE);

        section.addToggle(ToggleSetting.X_RAY, UiMessage.X_RAY);
        section.addToggle(ToggleSetting.V_SYNC, UiMessage.V_SYNC);

        section.addSlider(FloatSetting.REACH, UiMessage.REACH);
        section.addSlider(FloatSetting.BREAK_PLACE_INTERVALL, UiMessage.BREAK_PLACE_INTERVALL);

        return section;
    }

    private SettingsRenderable createDebugScreenSection() {
        SettingsRenderable section = new SettingsRenderable();

        for (DebugScreenLine debugLine : DebugScreenLine.getDebugLines()) section.addDebugLineSetting(debugLine);

        return section;
    }

    private Clickable getBackButtonAction() {
        return (Vector2i pixelCoordinate, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.popRenderable();
        };
    }

    private void addSection(int sectionNumber, SectionCreator sectionCreator, StringGetter name) {
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - sectionNumber * 0.15f);

        UiButton sectionButton = new UiButton(sizeToParent, offsetToParent, sectionButtonAction(sectionCreator));
        sectionButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), name));

        addRenderable(sectionButton);
        sectionButtons.add(sectionButton);
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
