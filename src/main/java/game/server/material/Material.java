package game.server.material;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.utils.FileManager;

import game.assets.TextureArrays;

import java.io.*;
import java.util.ArrayList;

import static game.utils.Constants.*;

public final class Material {

    public static void loadMaterials() {
        long start = System.nanoTime();
        AssetManager.reload(TextureArrays.MATERIALS);
        AssetManager.reload(TextureArrays.PROPERTIES);

        Gson gson = new Gson();
        for (MaterialIdentifier identifier : MaterialIdentifier.values()) loadMaterial(identifier, gson);
        System.out.printf("Loaded all materials. Took %sms%n", (System.nanoTime() - start) / 1_000_0000);
    }

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

    private static void loadMaterial(MaterialIdentifier identifier, Gson gson) {
        String json = FileManager.loadFileContents("assets/materials/%s.json".formatted(identifier.name()));
        Material material = gson.fromJson(json, Material.class);
        setMaterialData((byte) identifier.ordinal(), Properties.getCombinedValue(material.properties), material.textureIndex);
        System.out.println("Loaded Material " + identifier.name());
    }

    private static final byte[] MATERIAL_TEXTURE_INDICES = new byte[AMOUNT_OF_MATERIALS];
    private static final int[] MATERIAL_PROPERTIES = new int[AMOUNT_OF_MATERIALS];

    private Material() {
    }

    // Assigned with json-magic
    @SuppressWarnings("unused")
    private byte textureIndex;
    @SuppressWarnings("unused")
    private ArrayList<String> properties;
}
