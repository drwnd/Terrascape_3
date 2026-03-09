import core.languages.Language;
import core.rendering_api.Window;
import core.settings.Settings;

import game.languages.UiMessages;
import game.menus.MainMenu;
import game.server.material.Materials;
import game.settings.*;

public final class Launcher {

    public static void main(String[] args) {
        Settings.registerSettingsEnums(FloatSettings.class, IntSettings.class, KeySettings.class, ToggleSettings.class, OptionSettings.class);
        Language.registerTranslationEnums(Materials.class, UiMessages.class);
        Settings.loadFromFile();
        Window.init("Terrascape * 4096 remastered");
        Window.pushRenderable(new MainMenu());
        Window.renderLoop();
        Window.cleanUp();
    }
}
