package game.server;

import core.rendering_api.shaders.TextShader;
import core.settings.optionSettings.ColorOption;

public record ChatMessage(String message, Sender sender, ColorOption color, long timestamp, String[] lines) {

    /**
     * Constructs a new ChatMessage.
     * @param message the message text
     * @param sender the sender of the message
     * @param color the color of the message
     */
    public ChatMessage(String message, Sender sender, ColorOption color) {
        this(message, sender, color, System.nanoTime(), getLines(message));
    }

    public String prefix() {
        return sender.getPrefix();
    }

    /**
     * Splits the message into lines based on the maximum text length supported by the shader.
     * @param message the message text to split
     * @return an array of message lines
     */
    private static String[] getLines(String message) {
        String[] lines = new String[(int) (Math.ceil(message.length() / (double) TextShader.MAX_TEXT_LENGTH))];
        for (int index = 0; index < lines.length; index++) {
            int startIndex = index * TextShader.MAX_TEXT_LENGTH;
            int endIndex = Math.min((index + 1) * TextShader.MAX_TEXT_LENGTH, message.length());
            lines[index] = message.substring(startIndex, endIndex);
        }
        return lines;
    }
}
