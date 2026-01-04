package game.server.material;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.utils.FileManager;

import game.assets.TextureArrays;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class Material {

    public static void loadMaterials() {
        long start = System.nanoTime();
        AssetManager.reload(TextureArrays.MATERIALS);
        AssetManager.reload(TextureArrays.PROPERTIES);

        Gson gson = new Gson();
        for (MaterialIdentifier identifier : MaterialIdentifier.values()) loadMaterial(identifier, gson);
        System.out.printf("Loaded all materials. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
    }

    public static int getMaterialProperties(byte material) {
        return MATERIAL_PROPERTIES[material & 0xFF];
    }

    public static boolean isGlass(byte material) {
        return (material & 0xFF) >= (RED_GLASS & 0xFF) && (material & 0xFF) <= (BLACK_GLASS & 0xFF);
    }

    public static String getSystemName(byte material) {
        if ((material & 0xFF) >= AMOUNT_OF_MATERIALS) return "UNKNOWN";
        return MaterialIdentifier.values()[material & 0xFF].name();
    }

    private static void setMaterialData(byte material, byte properties) {
        MATERIAL_PROPERTIES[material & 0xFF] = properties;
    }

    private static void loadMaterial(MaterialIdentifier identifier, Gson gson) {
        String json = FileManager.loadJson("assets/materials/%s.json".formatted(identifier.name()));
        Material material = gson.fromJson(json, Material.class);
        setMaterialData((byte) identifier.ordinal(), Properties.getCombinedValue(material.properties));
        System.out.println("Loaded Material " + identifier.name());
    }

    private static final byte[] MATERIAL_PROPERTIES = new byte[AMOUNT_OF_MATERIALS];

    private Material() {
    }

    // Assigned with json-magic
    @SuppressWarnings("unused")
    private ArrayList<String> properties;
}
