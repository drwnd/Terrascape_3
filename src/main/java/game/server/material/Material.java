package game.server.material;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.utils.FileManager;
import game.assets.MaterialSounds;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class Material {

    public static void loadMaterials() {
        long start = System.nanoTime();
        AssetManager.deleteAll();

        Gson gson = new Gson();
        for (Materials identifier : Materials.values()) loadMaterial(identifier, gson);
        System.out.printf("Loaded all materials. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
    }

    public static int getProperties(byte material) {
        return MATERIAL_PROPERTIES[material & 0xFF];
    }

    public static MaterialSounds getDigSounds(byte material) {
        return DIG_SOUNDS[material & 0xFF];
    }

    public static MaterialSounds getStepSounds(byte material) {
        return STEP_SOUNDS[material & 0xFF];
    }

    public static MaterialSounds getJumpSounds(byte material) {
        return JUMP_SOUNDS[material & 0xFF];
    }

    public static boolean isGlass(byte material) {
        return (material & 0xFF) >= (RED_GLASS & 0xFF) && (material & 0xFF) <= (BLACK_GLASS & 0xFF);
    }

    public static String getSystemName(byte material) {
        if ((material & 0xFF) >= AMOUNT_OF_MATERIALS) return "UNKNOWN";
        return Materials.values()[material & 0xFF].name();
    }

    private static void loadMaterial(Materials identifier, Gson gson) {
        String json = FileManager.loadJson("assets/materials/%s.json".formatted(identifier.name()));
        Material material = gson.fromJson(json, Material.class);
        int materialIndex = identifier.ordinal() & 0xFF;

        MATERIAL_PROPERTIES[materialIndex] = Properties.getCombinedValue(material.properties);
        DIG_SOUNDS[materialIndex] = MaterialSounds.get(material.digSounds);
        STEP_SOUNDS[materialIndex] = MaterialSounds.get(material.stepSounds);
        JUMP_SOUNDS[materialIndex] = MaterialSounds.get(material.jumpSounds);

        System.out.println("Loaded Material " + identifier.name());
    }

    private static final byte[] MATERIAL_PROPERTIES = new byte[AMOUNT_OF_MATERIALS];
    private static final MaterialSounds[] DIG_SOUNDS = new MaterialSounds[AMOUNT_OF_MATERIALS];
    private static final MaterialSounds[] STEP_SOUNDS = new MaterialSounds[AMOUNT_OF_MATERIALS];
    private static final MaterialSounds[] JUMP_SOUNDS = new MaterialSounds[AMOUNT_OF_MATERIALS];

    private Material() {
    }

    // Assigned with json-magic
    @SuppressWarnings("unused")
    private ArrayList<String> properties;
    @SuppressWarnings("unused")
    private String digSounds, stepSounds, jumpSounds;
}
