package game.server.saving;

import core.settings.optionSettings.ColorOption;
import core.utils.MainThread;
import core.utils.Saver;

import game.server.ChatMessage;
import game.server.Sender;
import game.server.Server;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;

public final class ServerSaver extends Saver<Server> {

    public static Path getSaveFileLocation(String worldName) {
        return Path.of("saves", worldName, "serverData");
    }

    public ServerSaver() {
        super(128);
    }

    @Override
    protected void save(Server server) {
        saveLong(server.getCurrentGameTick());
        saveFloat(server.getDayTime());
        ArrayList<ChatMessage> messages = server.getMessages();
        saveInt(messages.size());
        for (ChatMessage message : messages) {
            saveString(message.message());
            saveInt(message.color().getColor().getRGB());
            saveInt(message.sender().ordinal());
        }
    }

    @Override
    @MainThread
    protected Server load() {
        long currentGameTick = loadLong();
        float dayTime = loadFloat();

        int messageCount = loadInt();
        ArrayList<ChatMessage> messages = new ArrayList<>(messageCount);
        for (int counter = 0; counter < messageCount; counter++) {
            String messageNoPrefix = loadString();
            ColorOption color = ColorOption.fromColor(new Color(loadInt()));
            Sender sender = Sender.values()[loadInt()];
            messages.add(new ChatMessage(messageNoPrefix, sender, color));
        }

        return new Server(currentGameTick, dayTime, messages);
    }

    @Override
    @MainThread
    protected Server getDefault() {
        return new Server(0L, 1.0F, new ArrayList<>());
    }

    @Override
    protected int getVersionNumber() {
        return 3;
    }
}
