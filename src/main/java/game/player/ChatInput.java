package game.player;

import core.renderables.TextField;
import core.renderables.TextFieldInput;

import game.player.rendering.Renderer;
import game.server.ChatMessage;
import game.server.Game;

import game.server.Sender;
import game.settings.KeySettings;
import core.utils.MainThread;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class ChatInput extends TextFieldInput {

    @MainThread
    public ChatInput(TextField field) {
        super(field);
    }

    @MainThread
    public float getScroll() {
        return scroll;
    }

    @MainThread
    public void setInputMode() {
        setStandardInputMode();
        skipNextInput = true;
        messageIndex = 0;
        scroll = 0.0F;
        cursorIndex = field.getText().length();
    }

    @Override
    @MainThread
    public void unset() {
        scroll = 0;
    }

    @Override
    @MainThread
    public void mouseButtonCallback(long window, int button, int action, int mods) {

    }

    @Override
    @MainThread
    public void charCallback(long window, int codePoint) {
        if (!skipNextInput) super.charCallback(window, codePoint);
        else skipNextInput = false;
    }

    @Override
    @MainThread
    public void scrollCallback(long window, double xScroll, double yScroll) {
        scroll = Math.max((float) (scroll + yScroll * 0.05), 0.0F);
    }

    @Override
    @MainThread
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == KeySettings.TAKE_SCREENSHOT.keybind() && action == GLFW_PRESS) Renderer.takeScreenshot();
        if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
        if (key == GLFW_KEY_ESCAPE) Game.getPlayer().toggleChat();
        if (key == GLFW_KEY_ENTER) {
            Game.getServer().sendPlayerMessage(field.getText());
            replaceText("");
            Game.getPlayer().toggleChat();
        }
        if (key == GLFW_KEY_UP) replaceText(nextPlayerMessage(1));
        if (key == GLFW_KEY_DOWN) replaceText(nextPlayerMessage(-1));

        super.keyCallback(window, key, scancode, action, mods);
    }

    private String nextPlayerMessage(int increment) {
        ArrayList<ChatMessage> messages = Game.getServer().getMessages();
        do {
            messageIndex += increment;
            if (messageIndex <= 0 || messageIndex >= messages.size() + 1) {
                messageIndex = Math.clamp(messageIndex, 0, messages.size());
                return "";
            }
        }
        while (messages.get(messages.size() - messageIndex).sender() != Sender.PLAYER);
        return messages.get(messages.size() - messageIndex).message();
    }

    @MainThread
    private void replaceText(String text) {
        cursorIndex = text.length();
        field.setText(text);
    }

    private boolean skipNextInput = false;
    int messageIndex = 0;
    private float scroll = 0.0F;
}
