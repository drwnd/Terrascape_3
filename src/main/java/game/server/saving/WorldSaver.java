package game.server.saving;

import core.utils.Saver;
import game.server.World;

import java.nio.file.Path;
import java.util.Date;

public final class WorldSaver extends Saver<World> {

    public static Path getSaveFileLocation(String worldName) {
        return Path.of("saves/%s/worldData".formatted(worldName));
    }

    public WorldSaver() {
        super(24);
    }

    @Override
    protected void save(World world) {
        saveLong(world.seed);
        saveLong(world.created.getTime());
        saveLong(new Date().getTime());
    }

    @Override
    protected World load() {
        long seed = loadLong();
        Date created = new Date(loadLong());
        Date lastPlayed = new Date(loadLong());
        return new World(seed, created, lastPlayed, false);
    }

    @Override
    protected World loadOldVersion(int versionNumber) {
        if (versionNumber == 0) return new World(loadLong(), new Date(0), new Date(0), false);
        return getDefault();
    }

    @Override
    protected World getDefault() {
        return new World(0, new Date(0), new Date(0), false);
    }

    @Override
    protected int getVersionNumber() {
        return 1;
    }
}
