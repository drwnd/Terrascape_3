import core.rendering_api.Window;
import core.settings.Settings;

import game.menus.MainMenu;
import game.server.Game;
import game.settings.*;

public final class Launcher {

    public static void main(String[] args) {
        Settings.registerSettingsEnums(FloatSettings.class, IntSettings.class, KeySettings.class, ToggleSettings.class, OptionSettings.class);
        Window.init("Terrascape");
        Window.pushRenderable(new MainMenu());
        Window.renderLoop();
        Window.cleanUp();
        Game.cleanUp();
    }
}
