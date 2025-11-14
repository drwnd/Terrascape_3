package game.server;

import core.settings.optionSettings.ColorOption;

public record ChatMessage(String message, Sender sender, ColorOption color, long timestamp) {

    public ChatMessage(String message, Sender sender, ColorOption color) {
        this(message, sender, color, System.nanoTime());
    }

    @Override
    public String message() {
        return sender.getPrefix() + message;
    }

    public String messageNoPrefix() {
        return message;
    }
}
