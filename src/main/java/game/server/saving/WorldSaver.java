package game.server.saving;

import core.utils.MainThread;
import core.utils.Saver;
import game.server.World;
import game.server.generation.BiomeSamplers;
import game.server.generation.WorldGenerationSettings;
import game.utils.Utils;

import java.nio.file.Path;
import java.util.Date;

public final class WorldSaver extends Saver<World> {

    public static Path getSaveFileLocation(String worldName) {
        return Path.of("saves/%s/worldData".formatted(Utils.sanitizeFileName(worldName)));
    }

    public WorldSaver() {
        super(24);
    }

    @Override
    protected void save(World world) {
        saveLong(world.worldGenerationSettings.seed());
        saveInt(world.worldGenerationSettings.biomeSampler().ordinal());
        saveInt(world.worldGenerationSettings.blockSize());
        saveString(world.name);
        saveString(world.created);
        saveString(new Date().toString());
        saveLong(new Date().getTime());
    }

    @Override
    @MainThread
    protected World load() {
        long seed = loadLong();
        BiomeSamplers biomeSampler = BiomeSamplers.getSaved(loadInt());
        int blockSize = loadInt();
        String name = loadString();
        String created = loadString();
        String lastPlayed = loadString();
        long lastPlayedAsMs = loadLong();
        return new World(new WorldGenerationSettings(seed, biomeSampler, blockSize), name, created, lastPlayed, lastPlayedAsMs, false);
    }

    @Override
    @MainThread
    protected World loadOldVersion(int versionNumber) {
        if (versionNumber == 3) {
            long seed = loadLong();
            BiomeSamplers biomeSampler = BiomeSamplers.getSaved(loadInt());
            int blockSize = loadInt();
            Date created = new Date(loadLong());
            Date lastPlayed = new Date(loadLong());
            return new World(new WorldGenerationSettings(seed, biomeSampler, blockSize), "", created.toString(), lastPlayed.toString(), 0, false);
        }
        return getDefault();
    }

    @Override
    @MainThread
    protected World getDefault() {
        return new World(new WorldGenerationSettings(0, BiomeSamplers.DEFAULT, 1), "", new Date(0).toString(), new Date(0).toString(), 0, false);
    }

    @Override
    protected int getVersionNumber() {
        return 4;
    }
}
