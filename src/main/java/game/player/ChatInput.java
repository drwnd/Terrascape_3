package game.player;

import core.renderables.TextField;
import core.renderables.TextFieldInput;
import game.server.Game;
import org.lwjgl.glfw.GLFW;

public final class ChatInput extends TextFieldInput {

    public ChatInput(TextField field) {
        super(field);
    }

    public void skipNextInput() {
        skipNextInput = true;
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {

    }

    @Override
    public void charCallback(long window, int codePoint) {
        if (!skipNextInput) super.charCallback(window, codePoint);
        else skipNextInput = false;
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return;

        if (key == GLFW.GLFW_KEY_BACKSPACE) handleBackspace();
        if (key == GLFW.GLFW_KEY_ESCAPE) Game.getPlayer().toggleChat();
        if (key == GLFW.GLFW_KEY_ENTER) {
            Game.getServer().sendPlayerMessage(field.getText());
            field.setText("");
            Game.getPlayer().toggleChat();
        }
    }

    private boolean skipNextInput = false;
}
