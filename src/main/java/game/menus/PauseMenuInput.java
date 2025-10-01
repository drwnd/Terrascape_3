package game.menus;

import org.lwjgl.glfw.GLFW;
import core.rendering_api.Input;
import core.rendering_api.Window;
import game.server.Game;

public final class PauseMenuInput extends Input {

    public PauseMenuInput(PauseMenu menu) {
        super(menu);
        this.menu = menu;
    }


    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);

        menu.hoverOver(cursorPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action != GLFW.GLFW_PRESS) return;

        menu.clickOn(cursorPos, button, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            Window.popRenderable();
            Game.getServer().startTicks();
            Game.getPlayer().setInput();
        }
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final PauseMenu menu;
}
