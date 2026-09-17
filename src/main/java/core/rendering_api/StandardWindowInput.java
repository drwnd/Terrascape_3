package core.rendering_api;

import core.assets.AssetManager;
import core.renderables.Renderable;
import core.renderables.TextFieldInput;
import core.settings.*;
import core.utils.MainThread;

import static org.lwjgl.glfw.GLFW.*;

public final class StandardWindowInput extends Input {

    @Override
    @MainThread
    public void setInputMode() {

    }

    @Override
    @MainThread
    public void cursorPosCallback(long window, double xPos, double yPos) {

    }

    @Override
    @MainThread
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action == GLFW_RELEASE) Renderable.releaseSelectedDraggable();
        if (!(Window.getInput() instanceof TextFieldInput)) handleToggleKeybinds();
    }

    @Override
    @MainThread
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    @MainThread
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (Window.getInput() instanceof TextFieldInput) return;

        if (Input.isKeyPressed(CoreKeySettings.RESIZE_WINDOW)) Window.toggleFullScreen();
        if (Input.isKeyPressed(CoreKeySettings.RELOAD_ASSETS)) AssetManager.deleteAll();
        handleToggleKeybinds();
    }

    @Override
    @MainThread
    public void charCallback(long window, int codePoint) {

    }

    @MainThread
    private static void handleToggleKeybinds() {
        boolean settingUpdated = false;
        for (Setting setting : Settings.getSettings()) {
            if (setting instanceof ToggleSetting toggleSetting && Input.isKeyPressed(toggleSetting)) {
                toggleSetting.setValue(!toggleSetting.value());
                settingUpdated = true;
            }
            if (setting instanceof OptionSetting optionSetting) {
                if (Input.isKeyPressed(optionSetting.nextKeySetting())) {
                    optionSetting.setValue(optionSetting.value().next());
                    settingUpdated = true;
                }
                if (Input.isKeyPressed(optionSetting.previousKeySetting())) {
                    optionSetting.setValue(optionSetting.value().previous());
                    settingUpdated = true;
                }
            }
        }
        if (settingUpdated) Settings.writeToFile();
    }
}
