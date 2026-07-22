package game.player;

import core.renderables.TextField;
import core.renderables.TextFieldInput;

import game.server.ChatMessage;
import game.server.Game;

import game.server.Sender;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class ChatInput extends TextFieldInput {

    public ChatInput(TextField field) {
        super(field);
    }

    public float getScroll() {
        return scroll;
    }

    /**
     * Initializes the chat input mode, resetting the scroll and setting the cursor to the end of the current text.
     */
    @Override
    public void setInputMode() {
        setStandardInputMode();
        skipNextInput = true;
        messageIndex = 0;
        scroll = 0.0F;
        cursorIndex = field.getText().length();
    }

    @Override
    public void unset() {
        scroll = 0;
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {

    }

    /**
     * Handles character input, skipping the next input if {@code skipNextInput} is set.
     *
     * @param window    the window handle
     * @param codePoint the Unicode code point of the character
     */
    @Override
    public void charCallback(long window, int codePoint) {
        if (!skipNextInput) super.charCallback(window, codePoint);
        else skipNextInput = false;
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        scroll = Math.max((float) (scroll + yScroll * 0.05), 0.0F);
    }

    /**
     * Handles key input for the chat, including closing the chat, sending messages, and navigating message history.
     *
     * @param window   the window handle
     * @param key      the keyboard key
     * @param scancode the platform-specific scancode
     * @param action   the key action (press, release, repeat)
     * @param mods     bitfield of modifier keys
     */
    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
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

    /**
     * Retrieves the next message from the player's message history.
     *
     * @param increment the direction to move in history (positive for older, negative for newer)
     * @return the message string from history, or an empty string if out of bounds
     */
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

    /**
     * Replaces the current text in the chat field and moves the cursor to the end.
     *
     * @param text the new text to set
     */
    private void replaceText(String text) {
        cursorIndex = text.length();
        field.setText(text);
    }

    private boolean skipNextInput = false;
    int messageIndex = 0;
    private float scroll = 0.0F;
}
