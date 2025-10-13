package game.server.saving;

import core.utils.Saver;
import game.server.Server;

public final class ServerSaver extends Saver<Server> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/serverData".formatted(worldName);
    }

    public ServerSaver() {
        super(8);
    }

    @Override
    protected void save(Server server) {
        saveLong(server.getCurrentGameTick());
    }

    @Override
    protected Server load() {
        long currentGameTick = loadLong();
        return new Server(currentGameTick);
    }

    @Override
    protected Server getDefault() {
        return new Server(0);
    }

    @Override
    protected int getVersionNumber() {
        return 0;
    }
}
