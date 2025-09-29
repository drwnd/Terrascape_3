package game.server;


import static game.utils.Constants.*;

public final class Material {

    public static byte getTextureIndex(byte material) {
        return MATERIAL_TEXTURE_INDICES[material & 0xFF];
    }

    public static int getMaterialProperties(byte material) {
        return MATERIAL_PROPERTIES[material & 0xFF];
    }

    public static boolean isSemiTransparentMaterial(byte material) {
        return (material & 0xFF) >= (RED_GLASS & 0xFF) && (material & 0xFF) <= (BLACK_GLASS & 0xFF);
    }

    private static void setMaterialData(byte material, int properties, byte texture) {
        MATERIAL_TEXTURE_INDICES[material & 0xFF] = texture;
        MATERIAL_PROPERTIES[material & 0xFF] = properties;
    }

    private static void initMaterials() {
        setMaterialData(AIR, NO_COLLISION | REPLACEABLE | TRANSPARENT, (byte) 0x00);
        setMaterialData(OUT_OF_WORLD, TRANSPARENT, (byte) 0x00);

        setMaterialData(PATH_BLOCK, 0, (byte) 0xD4);
        setMaterialData(CACTUS, REQUIRES_BOTTOM_SUPPORT, (byte) 113);
        setMaterialData(WATER, NO_COLLISION | REPLACEABLE | BLAST_RESISTANT | TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0x40);
        setMaterialData(LAVA, NO_COLLISION | REPLACEABLE | BLAST_RESISTANT | OCCLUDES_SELF_ONLY, (byte) 0x81);

        setMaterialData(GRASS, 0, (byte) 0xD6);
        setMaterialData(DIRT, 0, (byte) 1);
        setMaterialData(STONE, 0, (byte) 2);
        setMaterialData(STONE_BRICKS, 0, (byte) 34);
        setMaterialData(COBBLESTONE, 0, (byte) 50);
        setMaterialData(CHISELED_STONE, 0, (byte) 81);
        setMaterialData(POLISHED_STONE, 0, (byte) 66);
        setMaterialData(CHISELED_POLISHED_STONE, 0, (byte) 82);
        setMaterialData(MUD, 0, (byte) 17);
        setMaterialData(ANDESITE, 0, (byte) 18);
        setMaterialData(SNOW, 0, (byte) 32);
        setMaterialData(SAND, HAS_GRAVITY, (byte) 33);
        setMaterialData(SANDSTONE, 0, (byte) -95);
        setMaterialData(POLISHED_SANDSTONE, 0, (byte) -94);
        setMaterialData(SLATE, 0, (byte) 48);
        setMaterialData(CHISELED_SLATE, 0, (byte) -128);
        setMaterialData(COBBLED_SLATE, 0, (byte) 114);
        setMaterialData(SLATE_BRICKS, 0, (byte) -126);
        setMaterialData(POLISHED_SLATE, 0, (byte) -96);
        setMaterialData(GRAVEL, HAS_GRAVITY, (byte) 65);
        setMaterialData(COURSE_DIRT, 0, (byte) 80);
        setMaterialData(CLAY, 0, (byte) 97);
        setMaterialData(MOSS, 0, (byte) 98);
        setMaterialData(ICE, 0, (byte) 96);
        setMaterialData(HEAVY_ICE, 0, (byte) 112);
        setMaterialData(COAL_ORE, 0, (byte) -112);
        setMaterialData(IRON_ORE, 0, (byte) -111);
        setMaterialData(DIAMOND_ORE, 0, (byte) -110);

        setMaterialData(OAK_LOG, 0, (byte) 3);
        setMaterialData(STRIPPED_OAK_LOG, 0, (byte) 19);
        setMaterialData(SPRUCE_LOG, 0, (byte) 4);
        setMaterialData(STRIPPED_SPRUCE_LOG, 0, (byte) 20);
        setMaterialData(DARK_OAK_LOG, 0, (byte) 5);
        setMaterialData(STRIPPED_DARK_OAK_LOG, 0, (byte) 21);
        setMaterialData(PINE_LOG, 0, (byte) 6);
        setMaterialData(STRIPPED_PINE_LOG, 0, (byte) 22);
        setMaterialData(REDWOOD_LOG, 0, (byte) 7);
        setMaterialData(STRIPPED_REDWOOD_LOG, 0, (byte) 23);
        setMaterialData(BLACK_WOOD_LOG, 0, (byte) 8);
        setMaterialData(STRIPPED_BLACK_WOOD_LOG, 0, (byte) 24);
        setMaterialData(BASALT, 0, (byte) 0xC6);

        setMaterialData(OAK_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x33);
        setMaterialData(SPRUCE_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x34);
        setMaterialData(DARK_OAK_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x35);
        setMaterialData(PINE_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x36);
        setMaterialData(REDWOOD_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x37);
        setMaterialData(BLACK_WOOD_LEAVES, OCCLUDES_SELF_ONLY, (byte) 0x38);

        setMaterialData(OAK_PLANKS, 0, (byte) 0x23);
        setMaterialData(SPRUCE_PLANKS, 0, (byte) 0x24);
        setMaterialData(DARK_OAK_PLANKS, 0, (byte) 0x25);
        setMaterialData(PINE_PLANKS, 0, (byte) 0x26);
        setMaterialData(REDWOOD_PLANKS, 0, (byte) 0x27);
        setMaterialData(BLACK_WOOD_PLANKS, 0, (byte) 0x28);
        setMaterialData(CRACKED_ANDESITE, 0, (byte) -93);

        setMaterialData(OBSIDIAN, BLAST_RESISTANT, (byte) 0xB4);
        setMaterialData(MOSSY_STONE, 0, (byte) -17);
        setMaterialData(MOSSY_ANDESITE, 0, (byte) -18);
        setMaterialData(MOSSY_STONE_BRICKS, 0, (byte) -19);
        setMaterialData(MOSSY_POLISHED_STONE, 0, (byte) -20);
        setMaterialData(MOSSY_CHISELED_POLISHED_STONE, 0, (byte) -21);
        setMaterialData(MOSSY_CHISELED_STONE, 0, (byte) -22);
        setMaterialData(MOSSY_SLATE, 0, (byte) -23);
        setMaterialData(MOSSY_COBBLED_SLATE, 0, (byte) -24);
        setMaterialData(MOSSY_SLATE_BRICKS, 0, (byte) -25);
        setMaterialData(MOSSY_CHISELED_SLATE, 0, (byte) -26);
        setMaterialData(MOSSY_POLISHED_SLATE, 0, (byte) -27);
        setMaterialData(MOSSY_POLISHED_SANDSTONE, 0, (byte) 0xE4);
        setMaterialData(MOSSY_SANDSTONE, 0, (byte) 0xE3);
        setMaterialData(MOSSY_OBSIDIAN, BLAST_RESISTANT, (byte) 0xE2);
        setMaterialData(MOSSY_CRACKED_ANDESITE, 0, (byte) 0xE1);
        setMaterialData(MOSSY_COBBLESTONE, 0, (byte) 0xE0);
        setMaterialData(MOSSY_SANDSTONE_BRICKS, 0, (byte) 0xF0);
        setMaterialData(MOSSY_RED_SANDSTONE, 0, (byte) 0xF1);
        setMaterialData(MOSSY_RED_POLISHED_SANDSTONE, 0, (byte) 0xF2);
        setMaterialData(MOSSY_RED_SANDSTONE_BRICKS, 0, (byte) 0xF3);
        setMaterialData(MOSSY_COBBLED_BLACKSTONE, 0, (byte) 0xF4);
        setMaterialData(MOSSY_BLACKSTONE_BRICKS, 0, (byte) 0xF5);
        setMaterialData(MOSSY_POLISHED_BLACKSTONE, 0, (byte) 0xC3);
        setMaterialData(MOSSY_BLACKSTONE, 0, (byte) 0x77);

        setMaterialData(SEA_LIGHT, 0, (byte) 0xC4);
        setMaterialData(PODZOL, 0, (byte) 0xD5);
        setMaterialData(RED_SAND, HAS_GRAVITY, (byte) 0xB2);
        setMaterialData(RED_POLISHED_SANDSTONE, 0, (byte) 0xB1);
        setMaterialData(RED_SANDSTONE, 0, (byte) 0xB0);
        setMaterialData(TERRACOTTA, 0, (byte) 0xDF);
        setMaterialData(SANDSTONE_BRICKS, 0, (byte) 0xB6);
        setMaterialData(RED_SANDSTONE_BRICKS, 0, (byte) 0xB3);
        setMaterialData(COBBLED_BLACKSTONE, 0, (byte) 0x73);
        setMaterialData(BLACKSTONE_BRICKS, 0, (byte) 0x74);
        setMaterialData(POLISHED_BLACKSTONE, 0, (byte) 0x75);
        setMaterialData(COAL_BLOCK, 0, (byte) 0xC0);
        setMaterialData(IRON_BLOCK, 0, (byte) 0xC1);
        setMaterialData(DIAMOND_BLOCK, 0, (byte) 0xC2);
        setMaterialData(BLACKSTONE, 0, (byte) 0x76);

        setMaterialData(RED_TERRACOTTA, 0, (byte) 0xDE);
        setMaterialData(GREEN_TERRACOTTA, 0, (byte) 0xDD);
        setMaterialData(BLUE_TERRACOTTA, 0, (byte) 0xDC);
        setMaterialData(YELLOW_TERRACOTTA, 0, (byte) 0xDB);
        setMaterialData(MAGENTA_TERRACOTTA, 0, (byte) 0xDA);
        setMaterialData(CYAN_TERRACOTTA, 0, (byte) 0xD9);
        setMaterialData(WHITE_TERRACOTTA, 0, (byte) 0xD8);
        setMaterialData(BLACK_TERRACOTTA, 0, (byte) 0xD7);

        setMaterialData(BLACK, 0, (byte) -9);
        setMaterialData(WHITE, 0, (byte) -8);
        setMaterialData(CYAN, 0, (byte) -7);
        setMaterialData(MAGENTA, 0, (byte) -6);
        setMaterialData(YELLOW, 0, (byte) -5);
        setMaterialData(BLUE, 0, (byte) -4);
        setMaterialData(GREEN, 0, (byte) -3);
        setMaterialData(RED, 0, (byte) -2);

        setMaterialData(RED_WOOL, 0, (byte) 0xCE);
        setMaterialData(GREEN_WOOL, 0, (byte) 0xCD);
        setMaterialData(BLUE_WOOL, 0, (byte) 0xCC);
        setMaterialData(YELLOW_WOOL, 0, (byte) 0xCB);
        setMaterialData(MAGENTA_WOOL, 0, (byte) 0xCA);
        setMaterialData(CYAN_WOOL, 0, (byte) 0xC9);
        setMaterialData(WHITE_WOOL, 0, (byte) 0xC8);
        setMaterialData(BLACK_WOOL, 0, (byte) 0xC7);

        setMaterialData(GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0x31);
        setMaterialData(RED_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xBE);
        setMaterialData(GREEN_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xBD);
        setMaterialData(BLUE_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xBC);
        setMaterialData(YELLOW_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xBB);
        setMaterialData(MAGENTA_GLASS, TRANSPARENT, (byte) 0xBA);
        setMaterialData(CYAN_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xB9);
        setMaterialData(WHITE_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xB8);
        setMaterialData(BLACK_GLASS, TRANSPARENT | OCCLUDES_SELF_ONLY, (byte) 0xB7);

        setMaterialData(WHITE_LIGHT, 0, (byte) 0x98);
        setMaterialData(CYAN_LIGHT, 0, (byte) 0x99);
        setMaterialData(MAGENTA_LIGHT, 0, (byte) 0x9A);
        setMaterialData(YELLOW_LIGHT, 0, (byte) 0x9B);
        setMaterialData(BLUE_LIGHT, 0, (byte) 0x9C);
        setMaterialData(GREEN_LIGHT, 0, (byte) 0x9D);
        setMaterialData(RED_LIGHT, 0, (byte) 0x9E);
    }

    //I don't know how to use JSON-Files, so just ignore it
    public static void init() {
        initMaterials();
    }

    private static final byte[] MATERIAL_TEXTURE_INDICES = new byte[AMOUNT_OF_MATERIALS];
    private static final int[] MATERIAL_PROPERTIES = new int[AMOUNT_OF_MATERIALS];

    private Material() {
    }
}
