package server.saving;

import server.Server;

public final class ServerSaver extends Saver<Server> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/serverData".formatted(worldName);
    }

    public ServerSaver() {
        super(8);
    }

    @Override
    void save(Server server) {
        saveLong(server.getCurrentGameTick());
    }

    @Override
    Server load() {
        long currentGameTick = loadLong();
        return new Server(currentGameTick);
    }

    @Override
    Server getDefault() {
        return new Server(0);
    }
}
