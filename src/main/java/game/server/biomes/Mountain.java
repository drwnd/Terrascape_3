package game.server.biomes;

import core.utils.MathUtils;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.WATER_LEVEL;
import static game.utils.Constants.*;

public final class Mountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int snowHeight = MathUtils.floor(data.feature * 512 + SNOW_LEVEL);
        int grassHeight = MathUtils.floor(data.feature * 512) + WATER_LEVEL;
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (totalY > snowHeight && !data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, SNOW);
        else if (data.isInsideSurfaceMaterialLevel(totalY, 8) && data.height <= grassHeight) data.store(inChunkX, inChunkY, inChunkZ, GRASS);
        else if (!data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth) && data.height <= grassHeight) data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        else return false;
        return true;
    }

    private static final int SNOW_LEVEL = WATER_LEVEL + 1456;
}
