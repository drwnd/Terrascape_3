package core.rendering_api;

import core.assets.AssetManager;
import core.settings.KeySetting;
import core.settings.Settings;

public final class StandardWindowInput extends Input {

    @Override
    public void setInputMode() {

    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {

    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {

    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (Input.isKeyPressed(KeySetting.RESIZE_WINDOW)) Window.toggleFullScreen();
        if (Input.isKeyPressed(KeySetting.RELOAD_ASSETS)) AssetManager.reload();
        if (Input.isKeyPressed(KeySetting.RELOAD_SETTINGS)) Settings.loadFromFile();
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }
}
