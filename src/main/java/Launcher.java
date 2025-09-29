import game.menus.MainMenu;
import core.rendering_api.Window;
import core.settings.Settings;

public final class Launcher {

    public static void main(String[] args) {
        Settings.loadFromFile();
        Window.init("Terrascape * 4096 remastered");
        Window.pushRenderable(new MainMenu());
        Window.renderLoop();
        Window.cleanUp();
    }
}
