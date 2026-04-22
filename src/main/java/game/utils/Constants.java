package game.utils;

import core.utils.MathUtils;

import game.server.material.Materials;
import game.server.material.Properties;

public final class Constants {

    static {
        boolean isPerformanceTest = "true".equals(System.getenv().get("isPerformanceTest"));
        if (isPerformanceTest) RENDER_DISTANCE = 62;
        else RENDER_DISTANCE = 10;
    }

    public static final float Z_NEAR = 0.8208F; // Just barely can't xRay
    public static final float Z_FAR = Float.POSITIVE_INFINITY;

    public static final int CHUNK_SIZE_BITS = 6;
    public static final int CHUNK_SIZE = 1 << CHUNK_SIZE_BITS;
    public static final int CHUNK_SIZE_PADDED = CHUNK_SIZE + 1;
    public static final int CHUNK_SIZE_MASK = CHUNK_SIZE - 1;
    public static final long MAX_CHUNKS_MASK = -1L >>> CHUNK_SIZE_BITS;

    // Make into settings later
    public static final byte NUMBER_OF_GENERATION_THREADS = 3;
    public static final byte RENDER_DISTANCE;

    public static final int RENDERED_WORLD_WIDTH = MathUtils.nextLargestPowOf2(RENDER_DISTANCE * 2 + 3);
    public static final int RENDERED_WORLD_WIDTH_MASK = RENDERED_WORLD_WIDTH - 1;
    public static final int RENDERED_WORLD_WIDTH_BITS = Integer.numberOfTrailingZeros(RENDERED_WORLD_WIDTH);
    public static final int CHUNKS_PER_LOD = RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH;

    public static final int LOD_COUNT = 10;
    public static final int MAX_TREE_LOD = 4;

    public static final int SHADOW_MAP_SIZE = 2048;
    public static final int SHADOW_RANGE = 1024;
    public static final int SHADOW_LOD = 1;

    // Indices for the sides of blocks
    /**
     * Positive Z.
     */
    public static final byte NORTH = 0;
    /**
     * Positive Y.
     */
    public static final byte TOP = 1;
    /**
     * Positive X.
     */
    public static final byte WEST = 2;
    /**
     * Negative Z.
     */
    public static final byte SOUTH = 3;
    /**
     * Negative Y.
     */
    public static final byte BOTTOM = 4;
    /**
     * Negative X.
     */
    public static final byte EAST = 5;

    public static final int X_COMPONENT = 0;
    public static final int Y_COMPONENT = 1;
    public static final int Z_COMPONENT = 2;

    // Block Properties. Used in performance critical situations, so they are pulled out of their wrapper classes
    public static final byte NO_COLLISION = Properties.NO_COLLISION.getValue();
    public static final byte TRANSPARENT = Properties.TRANSPARENT.getValue();
    public static final byte OCCLUDES_SELF_ONLY = Properties.OCCLUDES_SELF_ONLY.getValue();
    public static final byte STRUCTURE_REPLACEABLE = Properties.STRUCTURE_REPLACEABLE.getValue();

    // All materials
    public static final byte AIR = 0;
    public static final byte OUT_OF_WORLD = 1;
    public static final byte WATER = 4;
    public static final byte LAVA = 5;
    public static final byte GRASS = 6;
    public static final byte DIRT = 7;
    public static final byte STONE = 8;
    public static final byte MUD = 14;
    public static final byte ANDESITE = 15;
    public static final byte SNOW = 16;
    public static final byte SAND = 17;
    public static final byte SANDSTONE = 18;
    public static final byte SLATE = 20;
    public static final byte GRAVEL = 26;
    public static final byte COURSE_DIRT = 27;
    public static final byte CLAY = 28;
    public static final byte MOSS = 29;
    public static final byte ICE = 30;
    public static final byte HEAVY_ICE = 31;
    public static final byte PODZOL = 86;
    public static final byte RED_SAND = 87;
    public static final byte RED_SANDSTONE = 88;
    public static final byte TERRACOTTA = 90;
    public static final byte RED_TERRACOTTA = 91;
    public static final byte YELLOW_TERRACOTTA = 94;
    public static final byte BLACKSTONE = 123;
    public static final byte RED_GLASS = 125;
    public static final byte BLACK_GLASS = -124;

    public static final int AMOUNT_OF_MATERIALS = Materials.values().length;

    // Just pretend it doesn't exist
    public static final float[] SKY_BOX_VERTICES;
    public static final int[] SKY_BOX_INDICES;
    public static final float[] SKY_BOX_TEXTURE_COORDINATES;

    private Constants() {
    }

    // No like actually, this doesn't exist! Trust me. please...
    static {
        // NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
        // I WARNED YOU!!!
        // WHY DIDN'T YOU LISTEN!??!!?
        // Ok it's actually not THAT bad... (anymore)

        SKY_BOX_VERTICES = new float[]{-1, -1, -1, -1, -1, 1, -1, 1, -1, -1, 1, 1, 1, -1, -1, 1, -1, 1, 1, 1, -1, 1, 1, 1, -1, -1, -1, -1, -1, 1, 1, -1, -1, 1, -1, 1, -1, 1, -1, -1, 1, 1, 1, 1, -1, 1, 1, 1, -1, -1, -1, -1, 1, -1, 1, -1, -1, 1, 1, -1, -1, -1, 1, -1, 1, 1, 1, -1, 1, 1, 1, 1};
        SKY_BOX_INDICES = new int[]{0, 2, 1, 3, 1, 2, 4, 5, 6, 7, 6, 5, 8, 9, 10, 11, 10, 9, 12, 14, 13, 15, 13, 14, 16, 18, 17, 19, 17, 18, 20, 21, 22, 23, 22, 21};
        SKY_BOX_TEXTURE_COORDINATES = new float[]{1, 0.6666667F, 0.75F, 0.6666667F, 1, 0.33333334F, 0.75F, 0.33333334F, 0.25F, 0.6666667F, 0.5F, 0.6666667F, 0.25F, 0.33333334F, 0.5F, 0.33333334F, 0.25F, 1, 0.5F, 1, 0.25F, 0.6666667F, 0.5F, 0.6666667F, 0.25F, 0, 0.5F, 0, 0.25F, 0.33333334F, 0.5F, 0.33333334F, 0, 0.6666667F, 0, 0.33333334F, 0.25F, 0.6666667F, 0.25F, 0.33333334F, 0.75F, 0.6666667F, 0.75F, 0.33333334F, 0.5F, 0.6666667F, 0.5F, 0.33333334F};
    }
}
