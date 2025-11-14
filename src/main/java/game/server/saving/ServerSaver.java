package game.server.saving;

import core.utils.Saver;
import game.server.Server;

import java.util.ArrayList;

public final class ServerSaver extends Saver<Server> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/serverData".formatted(worldName);
    }

    public ServerSaver() {
        super(12);
    }

    @Override
    protected void save(Server server) {
        saveLong(server.getCurrentGameTick());
        saveFloat(server.getDayTime());
    }

    @Override
    protected Server load() {
        long currentGameTick = loadLong();
        float dayTime = loadFloat();
        return new Server(currentGameTick, dayTime, new ArrayList<>()); // TODO load and store chat messages
    }

    @Override
    protected Server getDefault() {
        return new Server(0L, 1.0F, new ArrayList<>());
    }

    @Override
    protected int getVersionNumber() {
        return 1;
    }
}
