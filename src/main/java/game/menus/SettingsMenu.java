package game.menus;

import core.settings.CoreFloatSettings;
import core.settings.CoreKeySettings;
import core.settings.CoreOptionSettings;
import core.settings.CoreToggleSettings;
import core.utils.Message;
import core.utils.StringGetter;
import core.languages.UiMessage;
import core.renderables.*;
import core.rendering_api.Window;

import game.player.rendering.DebugScreenLine;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class SettingsMenu extends UiBackgroundElement {

    public SettingsMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        UiButton backButton = new UiButton(new Vector2f(0.25F, 0.1F), new Vector2f(0.05F, 0.85F), getBackButtonAction());
        backButton.addRenderable(new TextElement(new Vector2f(0.15F, 0.5F), UiMessage.BACK));
        addRenderable(backButton);

        int index = 0;
        addSection(++index, SettingsMenu::createControlsSection, UiMessage.CONTROLS_SECTION);
        addSection(++index, SettingsMenu::createRenderingSection, UiMessage.RENDERING_SECTION);
        addSection(++index, SettingsMenu::createUiSection, UiMessage.UI_CUSTOMIZATION_SECTION);
        addSection(++index, SettingsMenu::createSoundSection, UiMessage.SOUND_SECTION);
        addSection(++index, SettingsMenu::createDebugSection, UiMessage.DEBUG_SECTION);
        addSection(++index, SettingsMenu::createDebugScreenSection, UiMessage.DEBUG_SCREEN_SECTION);
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

        section.addToggle(CoreToggleSettings.SCROLL_HOTBAR, UiMessage.SCROLL_HOTBAR);
        section.addToggle(CoreToggleSettings.RAW_MOUSE_INPUT, UiMessage.RAW_MOUSE_INPUT);

        section.addSlider(CoreFloatSettings.SENSITIVITY, UiMessage.SENSITIVITY);

        section.addKeySelector(CoreKeySettings.MOVE_FORWARD, UiMessage.MOVE_FORWARD);
        section.addKeySelector(CoreKeySettings.MOVE_BACK, UiMessage.MOVE_BACK);
        section.addKeySelector(CoreKeySettings.MOVE_RIGHT, UiMessage.MOVE_RIGHT);
        section.addKeySelector(CoreKeySettings.MOVE_LEFT, UiMessage.MOVE_LEFT);
        section.addKeySelector(CoreKeySettings.JUMP, UiMessage.JUMP);
        section.addKeySelector(CoreKeySettings.SPRINT, UiMessage.SPRINT);
        section.addKeySelector(CoreKeySettings.SNEAK, UiMessage.SNEAK);
        section.addKeySelector(CoreKeySettings.CRAWL, UiMessage.CRAWL);
        section.addKeySelector(CoreKeySettings.FLY_FAST, UiMessage.FLY_FAST);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_1, UiMessage.HOTBAR_SLOT_1);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_2, UiMessage.HOTBAR_SLOT_2);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_3, UiMessage.HOTBAR_SLOT_3);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_4, UiMessage.HOTBAR_SLOT_4);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_5, UiMessage.HOTBAR_SLOT_5);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_6, UiMessage.HOTBAR_SLOT_6);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_7, UiMessage.HOTBAR_SLOT_7);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_8, UiMessage.HOTBAR_SLOT_8);
        section.addKeySelector(CoreKeySettings.HOTBAR_SLOT_9, UiMessage.HOTBAR_SLOT_9);
        section.addKeySelector(CoreKeySettings.DESTROY, UiMessage.DESTROY);
        section.addKeySelector(CoreKeySettings.USE, UiMessage.USE);
        section.addKeySelector(CoreKeySettings.PICK_BLOCK, UiMessage.PICK_BLOCK);
        section.addKeySelector(CoreKeySettings.INVENTORY, UiMessage.INVENTORY);
        section.addKeySelector(CoreKeySettings.LOCK_PLACE_POSITION, UiMessage.LOCK_PLACE_POSITION);
        section.addKeySelector(CoreKeySettings.ZOOM, UiMessage.ZOOM);
        section.addKeySelector(CoreKeySettings.INCREASE_BREAK_PLACE_SIZE, UiMessage.INCREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(CoreKeySettings.DECREASE_BREAK_PLACE_SIZE, UiMessage.DECREASE_BREAK_PLACE_SIZE);
        section.addKeySelector(CoreKeySettings.DROP, UiMessage.DROP);
        section.addKeySelector(CoreKeySettings.OPEN_CHAT, UiMessage.OPEN_CHAT);
        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW, UiMessage.RESIZE_WINDOW);

        return section;
    }

    private static SettingsRenderable createRenderingSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(CoreFloatSettings.FOV, UiMessage.FOV);
        section.addSlider(CoreFloatSettings.CROSSHAIR_SIZE, UiMessage.CROSSHAIR_SIZE);
        section.addSlider(CoreFloatSettings.HOTBAR_SIZE, UiMessage.HOTBAR_SIZE);
        section.addSlider(CoreFloatSettings.NIGHT_BRIGHTNESS, UiMessage.NIGHT_BRIGHTNESS);
        section.addSlider(CoreFloatSettings.AMBIENT_OCCLUSION_SAMPLES, UiMessage.AMBIENT_OCCLUSION_SAMPLES);

        section.addToggle(CoreToggleSettings.USE_SHADOW_MAPPING, UiMessage.USE_SHADOW_MAPPING);
        section.addToggle(CoreToggleSettings.CHUNKS_CAST_SHADOWS, UiMessage.CHUNKS_CAST_SHADOWS);
        section.addToggle(CoreToggleSettings.PARTICLES_CAST_SHADOWS, UiMessage.PARTICLES_CAST_SHADOWS);
        section.addToggle(CoreToggleSettings.GLASS_CASTS_SHADOWS, UiMessage.GLASS_CASTS_SHADOWS);
        section.addToggle(CoreToggleSettings.USE_AMBIENT_OCCLUSION, UiMessage.USE_AMBIENT_OCCLUSION);
        section.addToggle(CoreToggleSettings.SHOW_BREAK_PARTICLES, UiMessage.SHOW_BREAK_PARTICLES);
        section.addToggle(CoreToggleSettings.SHOW_CUBE_PLACE_PARTICLES, UiMessage.SHOW_CUBE_PLACE_PARTICLES);
        section.addToggle(CoreToggleSettings.SHOW_STRUCTURE_PLACE_PARTICLES, UiMessage.SHOW_STRUCTURE_PLACE_PARTICLES);
        section.addToggle(CoreToggleSettings.SHOW_SPLASH_PARTICLES, UiMessage.SHOW_SPLASH_PARTICLES);
        section.addToggle(CoreToggleSettings.USE_OCCLUSION_CULLING, UiMessage.USE_OCCLUSION_CULLING);

        return section;
    }

    private static SettingsRenderable createUiSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addOption(CoreOptionSettings.LANGUAGE, UiMessage.LANGUAGE);
        section.addOption(CoreOptionSettings.FONT, UiMessage.FONT);
        section.addOption(CoreOptionSettings.TEXTURE_PACK, UiMessage.TEXTURE_PACK);

        section.addSlider(CoreFloatSettings.GUI_SIZE, UiMessage.GUI_SIZE);
        section.addSlider(CoreFloatSettings.TEXT_SIZE, UiMessage.TEXT_SIZE);
        section.addSlider(CoreFloatSettings.RIM_THICKNESS, UiMessage.RIM_THICKNESS);
        section.addSlider(CoreFloatSettings.CHAT_MESSAGE_DURATION, UiMessage.CHAT_MESSAGE_DURATION);
        section.addSlider(CoreFloatSettings.MAX_CHAT_MESSAGE_COUNT, UiMessage.MAX_CHAT_MESSAGE_COUNT);
        section.addSlider(CoreFloatSettings.HOTBAR_INDICATOR_SCALER, UiMessage.HOTBAR_INDICATOR_SCALER);
        section.addSlider(CoreFloatSettings.PAUSE_MENU_BACKGROUND_BLUR, UiMessage.PAUSE_MENU_BACKGROUND_BLUR);
        section.addSlider(CoreFloatSettings.INVENTORY_ITEM_SIZE, UiMessage.INVENTORY_ITEM_SIZE);
        section.addSlider(CoreFloatSettings.INVENTORY_ITEMS_PER_ROW, UiMessage.INVENTORY_ITEMS_PER_ROW);
        section.addSlider(CoreFloatSettings.INVENTORY_ITEM_SCALING, UiMessage.INVENTORY_ITEM_SCALING);

        return section;
    }

    private static SettingsRenderable createSoundSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addSlider(CoreFloatSettings.MASTER_AUDIO, UiMessage.MASTER_AUDIO);
        section.addSlider(CoreFloatSettings.FOOTSTEPS_AUDIO, UiMessage.FOOTSTEPS_AUDIO);
        section.addSlider(CoreFloatSettings.PLACE_AUDIO, UiMessage.PLACE_AUDIO);
        section.addSlider(CoreFloatSettings.DIG_AUDIO, UiMessage.DIG_AUDIO);
        section.addSlider(CoreFloatSettings.INVENTORY_AUDIO, UiMessage.INVENTORY_AUDIO);
        section.addSlider(CoreFloatSettings.MISCELLANEOUS_AUDIO, UiMessage.MISCELLANEOUS_AUDIO);

        return section;
    }

    private static SettingsRenderable createDebugSection() {
        SettingsRenderable section = new SettingsRenderable();

        section.addKeySelector(CoreKeySettings.RESIZE_WINDOW, UiMessage.RESIZE_WINDOW);
        section.addKeySelector(CoreKeySettings.RELOAD_SETTINGS, new Message("Reload Settings"));
        section.addKeySelector(CoreKeySettings.RELOAD_ASSETS, new Message("Reload Assets"));
        section.addKeySelector(CoreKeySettings.RELOAD_LANGUAGE, new Message("Reload Language"));
        section.addKeySelector(CoreKeySettings.RELOAD_FONT, new Message("Reload Font"));
        section.addKeySelector(CoreKeySettings.RELOAD_MATERIALS, new Message("Reload Materials"));
        section.addKeySelector(CoreKeySettings.GET_CHUNK_REBUILD_PLACEABLE, new Message("Get Chunk Rebuilder"));
        section.addKeySelector(CoreKeySettings.START_COMMAND, new Message("Start Command"));

        section.addToggle(CoreToggleSettings.DEBUG_MENU, new Message("Open Debug Screen"));
        section.addToggle(CoreToggleSettings.NO_CLIP, new Message("No-Clip"));
        section.addToggle(CoreToggleSettings.CULLING_COMPUTATION, new Message("Culling Computation"));
        section.addToggle(CoreToggleSettings.X_RAY, new Message("X-Ray"));
        section.addToggle(CoreToggleSettings.V_SYNC, new Message("Use V-Sync"));
        section.addToggle(CoreToggleSettings.RENDER_OCCLUDERS, new Message("Render Occluders"));
        section.addToggle(CoreToggleSettings.RENDER_OCCLUDEES, new Message("Render Occludees"));
        section.addToggle(CoreToggleSettings.RENDER_OCCLUDER_DEPTH_MAP, new Message("Render Occluder Depth Map"));
        section.addToggle(CoreToggleSettings.RENDER_SHADOW_MAP, new Message("Render Shadow Map"));
        section.addToggle(CoreToggleSettings.RENDER_SHADOW_COLORS, new Message("Render Shadow Color"));

        section.addSlider(CoreFloatSettings.REACH, new Message("Reach"));
        section.addSlider(CoreFloatSettings.BREAK_PLACE_INTERVALL, new Message("Break Place Intervall"));
        section.addSlider(CoreFloatSettings.TIME_SPEED, new Message("Time Speed"));
        section.addSlider(CoreFloatSettings.DOWNWARD_SUN_DIRECTION, new Message("Downward Sun Direction"));
        section.addSlider(CoreFloatSettings.OCCLUDERS_OCCLUDEES_LOD, new Message("Occluders / Occludees debug lod"));

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
