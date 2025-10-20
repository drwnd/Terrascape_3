package game.utils;

import game.server.material.Properties;

public final class Constants {

    public static final float Z_NEAR = 0.8208f; // Just barely can't xRay
    public static final float Z_FAR = Float.POSITIVE_INFINITY;

    public static final int CHUNK_SIZE_BITS = 6;
    public static final int CHUNK_SIZE = 1 << CHUNK_SIZE_BITS;
    public static final int CHUNK_SIZE_PADDED = CHUNK_SIZE + 2;
    public static final int CHUNK_SIZE_MASK = CHUNK_SIZE - 1;
    public static final int MAX_CHUNKS_XZ = 0xFFFFFF;
    public static final int MAX_CHUNKS_Y = 0xFFFF;

    // Make into settings later
    public static final byte NUMBER_OF_GENERATION_THREADS = 3;
    public static final byte RENDER_DISTANCE_XZ = 6;
    public static final byte RENDER_DISTANCE_Y = 6;
    public static final byte RENDER_KEEP_DISTANCE = 2;

    public static final byte RENDERED_WORLD_WIDTH = RENDER_DISTANCE_XZ * 2 + 3 + RENDER_KEEP_DISTANCE * 2;
    public static final byte RENDERED_WORLD_HEIGHT = RENDER_DISTANCE_Y * 2 + 3 + RENDER_KEEP_DISTANCE * 2;

    public static final int LOD_COUNT = 10;

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

    // All materials
    public static final byte AIR = 0;
    public static final byte OUT_OF_WORLD = 1;
    public static final byte CACTUS = 2;
    public static final byte PATH_BLOCK = 3;
    public static final byte WATER = 4;
    public static final byte LAVA = 5;
    public static final byte GRASS = 6;
    public static final byte DIRT = 7;
    public static final byte STONE = 8;
    public static final byte STONE_BRICKS = 9;
    public static final byte COBBLESTONE = 10;
    public static final byte CHISELED_STONE = 11;
    public static final byte POLISHED_STONE = 12;
    public static final byte CHISELED_POLISHED_STONE = 13;
    public static final byte MUD = 14;
    public static final byte ANDESITE = 15;
    public static final byte SNOW = 16;
    public static final byte SAND = 17;
    public static final byte SANDSTONE = 18;
    public static final byte POLISHED_SANDSTONE = 19;
    public static final byte SLATE = 20;
    public static final byte CHISELED_SLATE = 21;
    public static final byte COBBLED_SLATE = 22;
    public static final byte SLATE_BRICKS = 23;
    public static final byte POLISHED_SLATE = 24;
    public static final byte GLASS = 25;
    public static final byte GRAVEL = 26;
    public static final byte COURSE_DIRT = 27;
    public static final byte CLAY = 28;
    public static final byte MOSS = 29;
    public static final byte ICE = 30;
    public static final byte HEAVY_ICE = 31;
    public static final byte COAL_ORE = 32;
    public static final byte IRON_ORE = 33;
    public static final byte DIAMOND_ORE = 34;
    public static final byte OAK_LOG = 35;
    public static final byte STRIPPED_OAK_LOG = 36;
    public static final byte SPRUCE_LOG = 37;
    public static final byte STRIPPED_SPRUCE_LOG = 38;
    public static final byte DARK_OAK_LOG = 39;
    public static final byte STRIPPED_DARK_OAK_LOG = 40;
    public static final byte PINE_LOG = 41;
    public static final byte STRIPPED_PINE_LOG = 42;
    public static final byte REDWOOD_LOG = 43;
    public static final byte STRIPPED_REDWOOD_LOG = 44;
    public static final byte BLACK_WOOD_LOG = 45;
    public static final byte STRIPPED_BLACK_WOOD_LOG = 46;
    public static final byte OAK_LEAVES = 47;
    public static final byte SPRUCE_LEAVES = 48;
    public static final byte DARK_OAK_LEAVES = 49;
    public static final byte PINE_LEAVES = 50;
    public static final byte REDWOOD_LEAVES = 51;
    public static final byte BLACK_WOOD_LEAVES = 52;
    public static final byte OAK_PLANKS = 53;
    public static final byte SPRUCE_PLANKS = 54;
    public static final byte DARK_OAK_PLANKS = 55;
    public static final byte PINE_PLANKS = 56;
    public static final byte REDWOOD_PLANKS = 57;
    public static final byte BLACK_WOOD_PLANKS = 58;
    public static final byte CRACKED_ANDESITE = 59;
    public static final byte BLACK = 60;
    public static final byte WHITE = 61;
    public static final byte CYAN = 62;
    public static final byte MAGENTA = 63;
    public static final byte YELLOW = 64;
    public static final byte BLUE = 65;
    public static final byte GREEN = 66;
    public static final byte RED = 67;
    public static final byte OBSIDIAN = 68;
    public static final byte MOSSY_STONE = 69;
    public static final byte MOSSY_ANDESITE = 70;
    public static final byte MOSSY_STONE_BRICKS = 71;
    public static final byte MOSSY_POLISHED_STONE = 72;
    public static final byte MOSSY_CHISELED_POLISHED_STONE = 73;
    public static final byte MOSSY_CHISELED_STONE = 74;
    public static final byte MOSSY_SLATE = 75;
    public static final byte MOSSY_COBBLED_SLATE = 76;
    public static final byte MOSSY_SLATE_BRICKS = 77;
    public static final byte MOSSY_CHISELED_SLATE = 78;
    public static final byte MOSSY_POLISHED_SLATE = 79;
    public static final byte MOSSY_SANDSTONE_BRICKS = 80;
    public static final byte MOSSY_RED_SANDSTONE_BRICKS = 81;
    public static final byte MOSSY_OBSIDIAN = 82;
    public static final byte MOSSY_CRACKED_ANDESITE = 83;
    public static final byte MOSSY_COBBLESTONE = 84;
    public static final byte SEA_LIGHT = 85;
    public static final byte PODZOL = 86;
    public static final byte RED_SAND = 87;
    public static final byte RED_SANDSTONE = 88;
    public static final byte RED_POLISHED_SANDSTONE = 89;
    public static final byte TERRACOTTA = 90;
    public static final byte RED_TERRACOTTA = 91;
    public static final byte GREEN_TERRACOTTA = 92;
    public static final byte BLUE_TERRACOTTA = 93;
    public static final byte YELLOW_TERRACOTTA = 94;
    public static final byte MAGENTA_TERRACOTTA = 95;
    public static final byte CYAN_TERRACOTTA = 96;
    public static final byte WHITE_TERRACOTTA = 97;
    public static final byte BLACK_TERRACOTTA = 98;
    public static final byte RED_WOOL = 99;
    public static final byte GREEN_WOOL = 100;
    public static final byte BLUE_WOOL = 101;
    public static final byte YELLOW_WOOL = 102;
    public static final byte MAGENTA_WOOL = 103;
    public static final byte CYAN_WOOL = 104;
    public static final byte WHITE_WOOL = 105;
    public static final byte BLACK_WOOL = 106;
    public static final byte SANDSTONE_BRICKS = 107;
    public static final byte RED_SANDSTONE_BRICKS = 108;
    public static final byte MOSSY_SANDSTONE = 109;
    public static final byte MOSSY_POLISHED_SANDSTONE = 110;
    public static final byte MOSSY_RED_SANDSTONE = 111;
    public static final byte MOSSY_RED_POLISHED_SANDSTONE = 112;
    public static final byte COBBLED_BLACKSTONE = 113;
    public static final byte BLACKSTONE_BRICKS = 114;
    public static final byte POLISHED_BLACKSTONE = 115;
    public static final byte COAL_BLOCK = 116;
    public static final byte IRON_BLOCK = 117;
    public static final byte DIAMOND_BLOCK = 118;
    public static final byte BASALT = 119;
    public static final byte MOSSY_COBBLED_BLACKSTONE = 120;
    public static final byte MOSSY_BLACKSTONE_BRICKS = 121;
    public static final byte MOSSY_POLISHED_BLACKSTONE = 122;
    public static final byte BLACKSTONE = 123;
    public static final byte MOSSY_BLACKSTONE = 124;
    public static final byte RED_GLASS = 125;
    public static final byte GREEN_GLASS = 126;
    public static final byte BLUE_GLASS = 127;
    public static final byte YELLOW_GLASS = -128;
    public static final byte MAGENTA_GLASS = -127;
    public static final byte CYAN_GLASS = -126;
    public static final byte WHITE_GLASS = -125;
    public static final byte BLACK_GLASS = -124;
    public static final byte RED_LIGHT = -123;
    public static final byte GREEN_LIGHT = -122;
    public static final byte BLUE_LIGHT = -121;
    public static final byte YELLOW_LIGHT = -120;
    public static final byte MAGENTA_LIGHT = -119;
    public static final byte CYAN_LIGHT = -118;
    public static final byte WHITE_LIGHT = -117;

    public static final int AMOUNT_OF_MATERIALS = 140;

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
        SKY_BOX_TEXTURE_COORDINATES = new float[]{1, 0.6666667f, 0.75f, 0.6666667f, 1, 0.33333334f, 0.75f, 0.33333334f, 0.25f, 0.6666667f, 0.5f, 0.6666667f, 0.25f, 0.33333334f, 0.5f, 0.33333334f, 0.25f, 1, 0.5f, 1, 0.25f, 0.6666667f, 0.5f, 0.6666667f, 0.25f, 0, 0.5f, 0, 0.25f, 0.33333334f, 0.5f, 0.33333334f, 0, 0.6666667f, 0, 0.33333334f, 0.25f, 0.6666667f, 0.25f, 0.33333334f, 0.75f, 0.6666667f, 0.75f, 0.33333334f, 0.5f, 0.6666667f, 0.5f, 0.33333334f};
    }
}
