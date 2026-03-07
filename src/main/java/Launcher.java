import core.rendering_api.Window;
import core.settings.Settings;

import game.menus.MainMenu;
import game.settings.FloatSettings;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import game.settings.OptionSettings;

public final class Launcher {

    public static void main(String[] args) {
        Settings.configureSettingsEnums(FloatSettings.class, KeySettings.class, ToggleSettings.class, OptionSettings.class);
        Settings.loadFromFile();
        Window.init("Terrascape * 4096 remastered");
        Window.pushRenderable(new MainMenu());
        Window.renderLoop();
        Window.cleanUp();
    }
}
