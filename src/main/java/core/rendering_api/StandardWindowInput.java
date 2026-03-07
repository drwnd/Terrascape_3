package core.rendering_api;

import core.assets.AssetManager;
import core.languages.Language;
import core.renderables.TextFieldInput;
import core.settings.CoreKeySettings;
import core.settings.CoreOptionSettings;
import core.settings.Settings;
import core.settings.CoreToggleSettings;
import core.settings.optionSettings.FontOption;

public final class StandardWindowInput extends Input {

    @Override
    public void setInputMode() {

    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {

    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (!(Window.getInput() instanceof TextFieldInput)) handleToggleKeybinds();
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (Input.isKeyPressed(CoreKeySettings.RESIZE_WINDOW)) Window.toggleFullScreen();
        if (Input.isKeyPressed(CoreKeySettings.RELOAD_ASSETS)) AssetManager.reload();
        if (Input.isKeyPressed(CoreKeySettings.RELOAD_SETTINGS)) Settings.loadFromFile();
        if (Input.isKeyPressed(CoreKeySettings.RELOAD_LANGUAGE)) ((Language) CoreOptionSettings.LANGUAGE.value()).load();
        if (Input.isKeyPressed(CoreKeySettings.RELOAD_FONT)) ((FontOption) CoreOptionSettings.FONT.value()).load();

        if (!(Window.getInput() instanceof TextFieldInput)) handleToggleKeybinds();
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private static void handleToggleKeybinds() {
        boolean settingUpdated = false;
        for (CoreToggleSettings setting : CoreToggleSettings.values()) {
            if (Input.isKeyPressed(setting)) Settings.update(setting, !setting.value());
            settingUpdated = true;
        }
        if (settingUpdated) Settings.writeToFile();
    }
}
