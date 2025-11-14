package game.server;

import core.settings.optionSettings.ColorOption;

public record ChatMessage(String message, Sender sender, ColorOption color, long timestamp) {

    public ChatMessage(String message, Sender sender, ColorOption color) {
        this(sender.getPrefix() + message, sender, color, System.nanoTime());
    }
}
