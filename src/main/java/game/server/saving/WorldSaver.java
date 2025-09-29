package game.server.saving;

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
    void save(World world) {
        saveLong(WorldGeneration.SEED);
    }

    @Override
    World load() {
        return new World(loadLong());
    }

    @Override
    World getDefault() {
        return new World(0);
    }
}
