package core.assets;

import core.assets.identifiers.*;

import java.util.HashMap;

public final class AssetManager {

    private AssetManager() {
    }


    public static void reload() {
        System.out.println("---Deleting old Assets---");
        synchronized (assets) {
            for (Asset asset : assets.values()) asset.delete();
            assets.clear();
        }
    }

    public static void reload(AssetIdentifier<?> identifier) {
        synchronized (assets) {
            if (!assets.containsKey(identifier)) return;
            Asset asset = assets.get(identifier);
            asset.delete();
            assets.remove(identifier, asset);
        }
    }

    @SuppressWarnings("unchecked")
    public static <ASSET extends Asset> ASSET get(AssetIdentifier<ASSET> identifier) {
        synchronized (assets) {
            if (assets.containsKey(identifier)) return (ASSET) assets.get(identifier);
            ASSET asset = identifier.getAssetGenerator().generate();
            assets.put(identifier, asset);
            return asset;
        }
    }


    private static final HashMap<AssetIdentifier<?>, Asset> assets = new HashMap<>();
}
