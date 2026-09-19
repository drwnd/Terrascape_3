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
        saveInt(world.worldGenerationSettings.biomeSampler().ordinal());
        saveInt(world.worldGenerationSettings.blockSize());
        saveLong(world.created.getTime());
        saveLong(new Date().getTime());
    }

    @Override
    @MainThread
    protected World load() {
        long seed = loadLong();
        BiomeSamplers preset = BiomeSamplers.getSaved(loadInt());
        int blockSize = loadInt();
        Date created = new Date(loadLong());
        Date lastPlayed = new Date(loadLong());
        return new World(new WorldGenerationSettings(seed, preset, blockSize), created, lastPlayed, false);
    }

    @Override
    @MainThread
    protected World loadOldVersion(int versionNumber) {
        if (versionNumber == 0)
            return new World(new WorldGenerationSettings(loadLong(), BiomeSamplers.DEFAULT, 1), new Date(0), new Date(0), false);
        if (versionNumber == 1)
            return new World(new WorldGenerationSettings(loadLong(), BiomeSamplers.DEFAULT, 1), new Date(loadLong()), new Date(loadLong()), false);
        if (versionNumber == 2)
            return new World(new WorldGenerationSettings(loadLong(), BiomeSamplers.getSaved(loadInt()), 1), new Date(loadLong()), new Date(loadLong()), false);
        return getDefault();
    }

    @Override
    @MainThread
    protected World getDefault() {
        return new World(new WorldGenerationSettings(0 ,BiomeSamplers.DEFAULT, 1) , new Date(0), new Date(0), false);
    }

    @Override
    protected int getVersionNumber() {
        return 2;
    }
}
