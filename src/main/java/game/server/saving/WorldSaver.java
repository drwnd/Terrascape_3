package game.server.saving;

import core.utils.MainThread;
import core.utils.Saver;
import game.server.World;
import game.server.generation.BiomeSamplers;
import game.server.generation.WorldGenerationSettings;

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
        saveLong(world.worldGenerationSettings.seed());
        saveLong(world.worldGenerationSettings.preset().ordinal());
        saveLong(world.created.getTime());
        saveLong(new Date().getTime());
    }

    @Override
    @MainThread
    protected World load() {
        long seed = loadLong();
        BiomeSamplers preset = BiomeSamplers.getSaved(loadInt());
        Date created = new Date(loadLong());
        Date lastPlayed = new Date(loadLong());
        return new World(new WorldGenerationSettings(preset, seed), created, lastPlayed, false);
    }

    @Override
    @MainThread
    protected World loadOldVersion(int versionNumber) {
        if (versionNumber == 0)
            return new World(new WorldGenerationSettings(BiomeSamplers.DEFAULT, loadLong()), new Date(0), new Date(0), false);
        if (versionNumber == 1)
            return new World(new WorldGenerationSettings(BiomeSamplers.DEFAULT, loadLong()), new Date(loadLong()), new Date(loadLong()), false);
        return getDefault();
    }

    @Override
    @MainThread
    protected World getDefault() {
        return new World(new WorldGenerationSettings(BiomeSamplers.DEFAULT, 0) , new Date(0), new Date(0), false);
    }

    @Override
    protected int getVersionNumber() {
        return 2;
    }
}
