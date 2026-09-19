package game.menus;

import core.rendering_api.MenuInput;
import core.settings.*;
import core.utils.StringGetter;
import core.language.CoreUiMessages;
import core.renderables.*;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.settings.*;

import core.utils.MainThread;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public final class SettingsMenu extends UiBackgroundElement {

    @MainThread
    public SettingsMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        UiButton backButton = new UiButton(new Vector2f(0.25F, 0.1F), new Vector2f(0.05F, 0.85F), Window::popRenderable);
        backButton.addRenderable(new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.BACK));
        addRenderable(backButton);

        int index = 0;
        addSection(++index, SettingsMenu::createControlsSection, UiMessages.CONTROLS_SECTION);
        addSection(++index, SettingsMenu::createRenderingSection, UiMessages.RENDERING_SECTION);
        addSection(++index, AssetPackSection::new, UiMessages.ASSET_PACKS_SECTION);
        addSection(++index, SettingsMenu::createUiSection, UiMessages.UI_CUSTOMIZATION_SECTION);
        addSection(++index, SettingsMenu::createSoundSection, UiMessages.SOUND_SECTION);
        addSection(++index, SettingsMenu::createDebugSection, UiMessages.DEBUG_SECTION);
        addSection(++index, SettingsMenu::createDebugScreenSection, UiMessages.DEBUG_SCREEN_SECTION);
    }

    @MainThread
    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0F : input.getScroll();
        input = new SettingsMenuInput(this);
        input.setScroll(scroll);
        Window.setInput(input);
    }


    @MainThread
    private static CoreSettingsRenderable createControlsSection() {
        CoreSettingsRenderable section = new CoreSettingsRenderable();

        section.addToggle(ToggleSettings.SCROLL_HOTBAR);
        section.addToggle(CoreToggleSettings.RAW_MOUSE_INPUT);

        section.addSlider(FloatSettings.SENSITIVITY);

        section.addKeySelector(KeySettings.MOVE_FORWARD);
        section.addKeySelector(KeySettings.MOVE_BACK);
        section.addKeySelector(KeySettings.MOVE_RIGHT);
        section.addKeySelector(KeySettings.MOVE_LEFT);
        section.addKeySelector(KeySettings.JUMP);
        section.addKeySelector(KeySettings.SPRINT);
        section.addKeySelector(KeySettings.SNEAK);
        section.addKeySelector(KeySettings.CRAWL);
        section.addKeySelector(KeySettings.FLY_FAST);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_1);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_2);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_3);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_4);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_5);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_6);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_7);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_8);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_9);
        section.addKeySelector(KeySettings.DESTROY);
        section.addKeySelector(KeySettings.USE);
        section.addKeySelector(KeySettings.PICK_BLOCK);
        section.addKeySelector(KeySettings.OPEN_INVENTORY);
        section.addKeySelector(KeySettings.SET_PLACE_START_POSITION);
        section.addKeySelector(KeySettings.LOCK_PLACE_POSITION);
        section.addKeySelector(KeySettings.SHOW_PLACEABLE_PREVIEW);
        section.addKeySelector(KeySettings.ROTATE_SHAPE_FORWARD);
        section.addKeySelector(KeySettings.ROTATE_SHAPE_BACKWARD);
        section.addKeySelector(KeySettings.ZOOM);
        section.addKeySelector(KeySettings.INCREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySettings.DECREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySettings.INCREASE_BREAK_PLACE_ALIGN);
        section.addKeySelector(KeySettings.DECREASE_BREAK_PLACE_ALIGN);
        section.addKeySelector(KeySettings.DROP);
        section.addKeySelector(KeySettings.OPEN_CHAT);
        section.addKeySelector(KeySettings.TAKE_SCREENSHOT);
        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW);

        return section;
    }

    @MainThread
    private static CoreSettingsRenderable createRenderingSection() {
        CoreSettingsRenderable section = new CoreSettingsRenderable();

        section.addSlider(FloatSettings.FOV);
        section.addSlider(IntSettings.RENDER_DISTANCE);
        section.addSlider(IntSettings.LOD_COUNT);
        section.addSlider(FloatSettings.NIGHT_BRIGHTNESS);
        section.addOption(OptionSettings.PERSPECTIVE);
        section.addSlider(FloatSettings.CROSSHAIR_SIZE);
        section.addSlider(FloatSettings.HOTBAR_SIZE);
        section.addSlider(IntSettings.AMBIENT_OCCLUSION_SAMPLES);
        section.addSlider(IntSettings.BREAK_PARTICLE_STEP_LENGTH);
        section.addSlider(IntSettings.PLACE_PARTICLE_STEP_LENGTH);
        section.addToggle(ToggleSettings.RENDER_HUD);
        section.addToggle(ToggleSettings.USE_SHADOW_MAPPING);
        section.addToggle(ToggleSettings.CHUNKS_CAST_SHADOWS);
        section.addToggle(ToggleSettings.PARTICLES_CAST_SHADOWS);
        section.addToggle(ToggleSettings.GLASS_CASTS_SHADOWS);
        section.addToggle(ToggleSettings.USE_AMBIENT_OCCLUSION);
        section.addToggle(ToggleSettings.SHOW_BREAK_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_SHAPE_PLACE_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_STRUCTURE_PLACE_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_SPLASH_PARTICLES);
        section.addToggle(ToggleSettings.HIDE_BODY_IN_FIRST_PERSON);
        section.addOption(OptionSettings.OCCLUSION_CULLING);

        return section;
    }

    @MainThread
    private static CoreSettingsRenderable createUiSection() {
        CoreSettingsRenderable section = new CoreSettingsRenderable();

        section.addOption(CoreOptionSettings.LANGUAGE);
        section.addOption(CoreOptionSettings.FONT);
        section.addOption(OptionSettings.SKIN);

        section.addSlider(CoreFloatSettings.GUI_SIZE);
        section.addSlider(CoreFloatSettings.TEXT_SIZE);
        section.addSlider(CoreFloatSettings.RIM_THICKNESS);
        section.addSlider(FloatSettings.CHAT_MESSAGE_DURATION);
        section.addSlider(IntSettings.MAX_CHAT_MESSAGE_COUNT);
        section.addSlider(FloatSettings.HOTBAR_INDICATOR_SCALER);
        section.addSlider(FloatSettings.PAUSE_MENU_BACKGROUND_BLUR);
        section.addSlider(FloatSettings.INVENTORY_ITEM_SIZE);
        section.addSlider(FloatSettings.INVENTORY_ITEM_SCALING);

        return section;
    }

    @MainThread
    private static CoreSettingsRenderable createSoundSection() {
        CoreSettingsRenderable section = new CoreSettingsRenderable();

        section.addSlider(CoreFloatSettings.MASTER_AUDIO);
        section.addSlider(FloatSettings.FOOTSTEPS_AUDIO);
        section.addSlider(FloatSettings.JUMP_AUDIO);
        section.addSlider(FloatSettings.PLACE_AUDIO);
        section.addSlider(FloatSettings.DIG_AUDIO);
        section.addSlider(FloatSettings.INVENTORY_AUDIO);
        section.addSlider(CoreFloatSettings.UI_AUDIO);

        return section;
    }

    @MainThread
    private static CoreSettingsRenderable createDebugSection() {
        CoreSettingsRenderable section = new CoreSettingsRenderable();

        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW);
        section.addKeySelector(CoreKeySettings.RELOAD_ASSETS);
        section.addKeySelector(KeySettings.START_COMMAND);

        section.addToggle(ToggleSettings.OPEN_DEBUG_MENU);
        section.addToggle(ToggleSettings.NO_CLIP);
        section.addToggle(ToggleSettings.CULLING_COMPUTATION);
        section.addToggle(ToggleSettings.TOGGLE_X_RAY);
        section.addToggle(CoreToggleSettings.V_SYNC);
        section.addToggle(ToggleSettings.RENDER_OCCLUDERS);
        section.addToggle(ToggleSettings.RENDER_OCCLUDEES);
        section.addToggle(ToggleSettings.RENDER_OCCLUDER_DEPTH_MAP);
        section.addToggle(ToggleSettings.RENDER_SHADOW_MAP);
        section.addToggle(ToggleSettings.RENDER_SHADOW_COLORS);
        section.addToggle(ToggleSettings.RENDER_ACCUMULATION_TEXTURE);
        section.addToggle(ToggleSettings.RENDER_REVEAL_TEXTURE);
        section.addOption(CoreOptionSettings.LOG_MESSAGES);

        section.addSlider(IntSettings.REACH);
        section.addSlider(IntSettings.BREAK_PLACE_INTERVALL);
        section.addSlider(FloatSettings.TIME_SPEED);
        section.addSlider(FloatSettings.DOWNWARD_SUN_DIRECTION);
        section.addSlider(IntSettings.OCCLUDERS_OCCLUDEES_LOD);

        return section;
    }

    @MainThread
    private static CoreSettingsRenderable createDebugScreenSection() {
        SettingsRenderable section = new SettingsRenderable();
        for (DebugScreenOptions debugOptions : DebugScreenOptions.values()) section.addDebugLineSetting(debugOptions);
        return section;
    }

    @MainThread
    private void addSection(int sectionNumber, SectionCreator sectionCreator, StringGetter name) {
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 0.975F - sectionNumber * 0.125F);

        UiButton sectionButton = new UiButton(sizeToParent, offsetToParent, () -> Window.pushRenderable(sectionCreator.getSection()));
        sectionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), name));

        addRenderable(sectionButton);
    }

    private SettingsMenuInput input;

    private interface SectionCreator {
        Renderable getSection();
    }

    private static final class SettingsMenuInput extends MenuInput<SettingsMenu> {

        @MainThread
        private SettingsMenuInput(SettingsMenu menu) {
            super(menu);
        }

        @Override
        @MainThread
        public void keyCallback(long window, int key, int scancode, int action, int mods) {
            if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) Window.popRenderable();
        }
    }
}
