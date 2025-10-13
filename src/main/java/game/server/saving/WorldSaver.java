package game.server.saving;

import core.utils.Saver;
import game.server.World;
import game.server.generation.WorldGeneration;

public final class WorldSaver extends Saver<World> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/worldData".formatted(worldName);
    }

    public WorldSaver() {
        super(8);
    }

    @Override
    protected void save(World world) {
        saveLong(WorldGeneration.SEED);
    }

    @Override
    protected World load() {
        return new World(loadLong());
    }

    @Override
    protected World getDefault() {
        return new World(0);
    }

    @Override
    protected int getVersionNumber() {
        return 0;
    }
}
