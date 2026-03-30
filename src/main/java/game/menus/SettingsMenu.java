package game.menus;

import core.settings.*;
import core.utils.Message;
import core.utils.StringGetter;
import core.language.CoreUiMessages;
import core.renderables.*;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.player.rendering.DebugScreenLine;

import game.settings.FloatSettings;
import game.settings.IntSettings;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class SettingsMenu extends UiBackgroundElement {

    public SettingsMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        UiButton backButton = new UiButton(new Vector2f(0.25F, 0.1F), new Vector2f(0.05F, 0.85F), getBackButtonAction());
        backButton.addRenderable(new TextElement(new Vector2f(0.15F, 0.5F), CoreUiMessages.BACK));
        addRenderable(backButton);

        int index = 0;
        addSection(++index, SettingsMenu::createControlsSection, UiMessages.CONTROLS_SECTION);
        addSection(++index, SettingsMenu::createRenderingSection, UiMessages.RENDERING_SECTION);
        addSection(++index, SettingsMenu::createUiSection, UiMessages.UI_CUSTOMIZATION_SECTION);
        addSection(++index, SettingsMenu::createSoundSection, UiMessages.SOUND_SECTION);
        addSection(++index, SettingsMenu::createDebugSection, UiMessages.DEBUG_SECTION);
        addSection(++index, SettingsMenu::createDebugScreenSection, UiMessages.DEBUG_SCREEN_SECTION);
    }

    public void scrollSectionButtons(float scroll) {
        Vector2f offset = new Vector2f(0.0F, scroll);

        for (Renderable renderable : sectionButtons) renderable.move(offset);
    }

    @Override
    public void setOnTop() {
        float scroll = input == null ? 0.0F : input.getScroll();
        input = new SettingsMenuInput(this);
        input.setScroll(scroll);
        Window.setInput(input);
    }


    private static SettingsRenderable createControlsSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addToggle(ToggleSettings.SCROLL_HOTBAR, UiMessages.SCROLL_HOTBAR);
        section.addToggle(CoreToggleSettings.RAW_MOUSE_INPUT, CoreUiMessages.RAW_MOUSE_INPUT);

        section.addSlider(CoreFloatSettings.SENSITIVITY, UiMessages.SENSITIVITY);

        section.addKeySelector(KeySettings.MOVE_FORWARD, UiMessages.MOVE_FORWARD);
        section.addKeySelector(KeySettings.MOVE_BACK, UiMessages.MOVE_BACK);
        section.addKeySelector(KeySettings.MOVE_RIGHT, UiMessages.MOVE_RIGHT);
        section.addKeySelector(KeySettings.MOVE_LEFT, UiMessages.MOVE_LEFT);
        section.addKeySelector(KeySettings.JUMP, UiMessages.JUMP);
        section.addKeySelector(KeySettings.SPRINT, UiMessages.SPRINT);
        section.addKeySelector(KeySettings.SNEAK, UiMessages.SNEAK);
        section.addKeySelector(KeySettings.CRAWL, UiMessages.CRAWL);
        section.addKeySelector(KeySettings.FLY_FAST, UiMessages.FLY_FAST);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_1, UiMessages.HOTBAR_SLOT_1);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_2, UiMessages.HOTBAR_SLOT_2);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_3, UiMessages.HOTBAR_SLOT_3);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_4, UiMessages.HOTBAR_SLOT_4);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_5, UiMessages.HOTBAR_SLOT_5);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_6, UiMessages.HOTBAR_SLOT_6);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_7, UiMessages.HOTBAR_SLOT_7);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_8, UiMessages.HOTBAR_SLOT_8);
        section.addKeySelector(KeySettings.HOTBAR_SLOT_9, UiMessages.HOTBAR_SLOT_9);
        section.addKeySelector(KeySettings.DESTROY, UiMessages.DESTROY);
        section.addKeySelector(KeySettings.USE, UiMessages.USE);
        section.addKeySelector(KeySettings.PICK_BLOCK, UiMessages.PICK_BLOCK);
        section.addKeySelector(KeySettings.INVENTORY, UiMessages.INVENTORY);
        section.addKeySelector(KeySettings.SET_PLACE_START_POSITION, UiMessages.SET_PLACE_START_POSITION);
        section.addKeySelector(KeySettings.SHOW_PLACEABLE_PREVIEW, UiMessages.SHOW_PLACEABLE_PREVIEW);
        section.addKeySelector(KeySettings.ROTATE_SHAPE_FORWARD, UiMessages.ROTATE_SHAPE_FORWARD);
        section.addKeySelector(KeySettings.ROTATE_SHAPE_BACKWARD, UiMessages.ROTATE_SHAPE_BACKWARD);
        section.addKeySelector(KeySettings.ZOOM, UiMessages.ZOOM);
        section.addKeySelector(KeySettings.INCREASE_BREAK_PLACE_SIZE, UiMessages.INCREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySettings.DECREASE_BREAK_PLACE_SIZE, UiMessages.DECREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(KeySettings.INCREASE_BREAK_PLACE_ALIGN, UiMessages.INCREASE_BREAK_PLACE_ALIGN);
        section.addKeySelector(KeySettings.DECREASE_BREAK_PLACE_ALIGN, UiMessages.DECREASE_BREAK_PLACE_ALIGN);
        section.addKeySelector(KeySettings.DROP, UiMessages.DROP);
        section.addKeySelector(KeySettings.OPEN_CHAT, UiMessages.OPEN_CHAT);
        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW, CoreUiMessages.RESIZE_WINDOW);

        return section;
    }

    private static SettingsRenderable createRenderingSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(FloatSettings.FOV, UiMessages.FOV);
        section.addSlider(FloatSettings.CROSSHAIR_SIZE, UiMessages.CROSSHAIR_SIZE);
        section.addSlider(FloatSettings.HOTBAR_SIZE, UiMessages.HOTBAR_SIZE);
        section.addSlider(FloatSettings.NIGHT_BRIGHTNESS, UiMessages.NIGHT_BRIGHTNESS);
        section.addSlider(IntSettings.AMBIENT_OCCLUSION_SAMPLES, UiMessages.AMBIENT_OCCLUSION_SAMPLES);

        section.addToggle(ToggleSettings.USE_SHADOW_MAPPING, UiMessages.USE_SHADOW_MAPPING);
        section.addToggle(ToggleSettings.CHUNKS_CAST_SHADOWS, UiMessages.CHUNKS_CAST_SHADOWS);
        section.addToggle(ToggleSettings.PARTICLES_CAST_SHADOWS, UiMessages.PARTICLES_CAST_SHADOWS);
        section.addToggle(ToggleSettings.GLASS_CASTS_SHADOWS, UiMessages.GLASS_CASTS_SHADOWS);
        section.addToggle(ToggleSettings.USE_AMBIENT_OCCLUSION, UiMessages.USE_AMBIENT_OCCLUSION);
        section.addToggle(ToggleSettings.SHOW_BREAK_PARTICLES, UiMessages.SHOW_BREAK_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_CUBE_PLACE_PARTICLES, UiMessages.SHOW_CUBE_PLACE_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_STRUCTURE_PLACE_PARTICLES, UiMessages.SHOW_STRUCTURE_PLACE_PARTICLES);
        section.addToggle(ToggleSettings.SHOW_SPLASH_PARTICLES, UiMessages.SHOW_SPLASH_PARTICLES);
        section.addToggle(ToggleSettings.USE_OCCLUSION_CULLING, UiMessages.USE_OCCLUSION_CULLING);

        return section;
    }

    private static SettingsRenderable createUiSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addOption(CoreOptionSettings.LANGUAGE, CoreUiMessages.LANGUAGE);
        section.addOption(CoreOptionSettings.FONT, CoreUiMessages.FONT);
        section.addOption(CoreOptionSettings.TEXTURE_PACK, CoreUiMessages.TEXTURE_PACK);

        section.addSlider(CoreFloatSettings.GUI_SIZE, CoreUiMessages.GUI_SIZE);
        section.addSlider(CoreFloatSettings.TEXT_SIZE, CoreUiMessages.TEXT_SIZE);
        section.addSlider(CoreFloatSettings.RIM_THICKNESS, CoreUiMessages.RIM_THICKNESS);
        section.addSlider(FloatSettings.CHAT_MESSAGE_DURATION, UiMessages.CHAT_MESSAGE_DURATION);
        section.addSlider(IntSettings.MAX_CHAT_MESSAGE_COUNT, UiMessages.MAX_CHAT_MESSAGE_COUNT);
        section.addSlider(FloatSettings.HOTBAR_INDICATOR_SCALER, UiMessages.HOTBAR_INDICATOR_SCALER);
        section.addSlider(FloatSettings.PAUSE_MENU_BACKGROUND_BLUR, UiMessages.PAUSE_MENU_BACKGROUND_BLUR);
        section.addSlider(FloatSettings.INVENTORY_ITEM_SIZE, UiMessages.INVENTORY_ITEM_SIZE);
        section.addSlider(FloatSettings.INVENTORY_ITEM_SCALING, UiMessages.INVENTORY_ITEM_SCALING);

        return section;
    }

    private static SettingsRenderable createSoundSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(CoreFloatSettings.MASTER_AUDIO, CoreUiMessages.MASTER_AUDIO);
        section.addSlider(FloatSettings.FOOTSTEPS_AUDIO, UiMessages.FOOTSTEPS_AUDIO);
        section.addSlider(FloatSettings.PLACE_AUDIO, UiMessages.PLACE_AUDIO);
        section.addSlider(FloatSettings.DIG_AUDIO, UiMessages.DIG_AUDIO);
        section.addSlider(FloatSettings.INVENTORY_AUDIO, UiMessages.INVENTORY_AUDIO);
        section.addSlider(CoreFloatSettings.MISCELLANEOUS_AUDIO, CoreUiMessages.MISCELLANEOUS_AUDIO);

        return section;
    }

    private static SettingsRenderable createDebugSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW, CoreUiMessages.RESIZE_WINDOW);
        section.addKeySelector(CoreKeySettings.RELOAD_SETTINGS, new Message("Reload Settings"));
        section.addKeySelector(CoreKeySettings.RELOAD_ASSETS, new Message("Reload Assets"));
        section.addKeySelector(CoreKeySettings.RELOAD_FONT, new Message("Reload Font"));
        section.addKeySelector(KeySettings.RELOAD_MATERIALS, new Message("Reload Materials"));
        section.addKeySelector(KeySettings.GET_CHUNK_REBUILD_PLACEABLE, new Message("Get Chunk Rebuilder"));
        section.addKeySelector(KeySettings.START_COMMAND, new Message("Start Command"));

        section.addToggle(ToggleSettings.DEBUG_MENU, new Message("Open Debug Screen"));
        section.addToggle(ToggleSettings.NO_CLIP, new Message("No-Clip"));
        section.addToggle(ToggleSettings.CULLING_COMPUTATION, new Message("Culling Computation"));
        section.addToggle(ToggleSettings.X_RAY, new Message("X-Ray"));
        section.addToggle(CoreToggleSettings.V_SYNC, new Message("Use V-Sync"));
        section.addToggle(ToggleSettings.RENDER_OCCLUDERS, new Message("Render Occluders"));
        section.addToggle(ToggleSettings.RENDER_OCCLUDEES, new Message("Render Occludees"));
        section.addToggle(ToggleSettings.RENDER_OCCLUDER_DEPTH_MAP, new Message("Render Occluder Depth Map"));
        section.addToggle(ToggleSettings.RENDER_SHADOW_MAP, new Message("Render Shadow Map"));
        section.addToggle(ToggleSettings.RENDER_SHADOW_COLORS, new Message("Render Shadow Color"));

        section.addSlider(IntSettings.REACH, new Message("Reach"));
        section.addSlider(IntSettings.BREAK_PLACE_INTERVALL, new Message("Break Place Intervall"));
        section.addSlider(FloatSettings.TIME_SPEED, new Message("Time Speed"));
        section.addSlider(FloatSettings.DOWNWARD_SUN_DIRECTION, new Message("Downward Sun Direction"));
        section.addSlider(IntSettings.OCCLUDERS_OCCLUDEES_LOD, new Message("Occluders / Occludees debug lod"));

        return section;
    }

    private static SettingsRenderable createDebugScreenSection() {
        SettingsRenderable section = new SettingsRenderable();

        for (DebugScreenLine debugLine : DebugScreenLine.getDebugLines()) section.addDebugLineSetting(debugLine);

        return section;
    }

    private static Clickable getBackButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            Window.popRenderable();
        };
    }

    private void addSection(int sectionNumber, SectionCreator sectionCreator, StringGetter name) {
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - sectionNumber * 0.15F);

        UiButton sectionButton = new UiButton(sizeToParent, offsetToParent, sectionButtonAction(sectionCreator));
        sectionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), name));

        addRenderable(sectionButton);
        sectionButtons.add(sectionButton);
    }

    private static Clickable sectionButtonAction(SectionCreator sectionCreator) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            Window.pushRenderable(sectionCreator.getSection());
        };
    }

    private SettingsMenuInput input;
    private final ArrayList<UiButton> sectionButtons = new ArrayList<>();

    private interface SectionCreator {
        SettingsRenderable getSection();
    }
}
