import core.language.Language;
import core.rendering_api.Window;

import core.settings.Settings;
import game.language.UiMessages;
import game.menus.MainMenu;
import game.server.material.Materials;
import game.settings.*;

public final class Launcher {

    public static void main(String[] args) {
        Settings.registerSettingsEnums(FloatSettings.class, IntSettings.class, KeySettings.class, ToggleSettings.class, OptionSettings.class);
        Language.registerTranslationEnums(Materials.class, UiMessages.class);
        Window.init("Terrascape 3");
        Window.pushRenderable(new MainMenu());
        Window.renderLoop();
        Window.cleanUp();
    }
}
