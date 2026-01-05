package game.assets;

import core.assets.Asset;

import game.server.generation.Structure;

import static game.utils.Constants.*;

public final class StructureCollection extends Asset {

    public StructureCollection(Structure[] structures) {
        this.structures = structures;
    }

    public Structure getRandom(int x, int y, int z) {
        return structures[(x + y + z >>> CHUNK_SIZE_BITS) % structures.length];
    }

    @Override
    public void delete() {

    }

    private final Structure[] structures;
}
