package core.assets;

import core.assets.identifiers.*;
import core.rendering_api.Debug;
import core.utils.FileManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public final class AssetManager {

    private AssetManager() {
    }


    public static void cleanUp() {
        Debug.log("---Deleting old Assets---");
        synchronized (assets) {
            for (Asset asset : assets.values()) asset.delete();
            assets.clear();
        }
        deleteAllCallbacks.clear();
        assetPackNames.clear();
    }

    public static void deleteAll() {
        Debug.log("---Deleting old Assets---");
        synchronized (assets) {
            for (Asset asset : assets.values()) asset.delete();
            assets.clear();
        }
        for (Runnable callback : deleteAllCallbacks) callback.run();
    }

    public static void delete(AssetIdentifier<?> identifier) {
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
            ASSET asset = identifier.generateAsset();
            assets.put(identifier, asset);
            return asset;
        }
    }

    public static Path getAssetFilepath(Path assetPath) {
        for (String assetPackName : assetPackNames) {
            Path filepath = Path.of("assetPacks", assetPackName).resolve(assetPath);
            if (filepath.toFile().exists()) return filepath;
        }
        return Path.of("assetPacks", "Default").resolve(assetPath);
    }

    public static ArrayList<Path> getAssetFilePathsInFolder(String folderName) {
        ArrayList<Path> filePaths = new ArrayList<>();
        for (String assetPackName : assetPackNames)
            addAssetFilePathsInFolder(filePaths, Path.of("assetPacks", assetPackName, folderName));
        addAssetFilePathsInFolder(filePaths, Path.of("assetPacks", "Default", folderName));
        return filePaths;
    }

    public static ArrayList<Path> getAssetFilePathsInFolderMatching(String folderName, String fileNamePrefix) {
        ArrayList<Path> assetFilesInFolder = getAssetFilePathsInFolder(folderName);
        assetFilesInFolder.removeIf(path -> {
            String fileName = path.getFileName().toString();
            return !fileName.startsWith(fileNamePrefix);
        });
        return assetFilesInFolder;
    }

    public static void addDeleteAllCallback(Runnable callback) {
        if (callback == null) return;
        deleteAllCallbacks.add(callback);
    }

    public static ArrayList<String> getActiveAssetPackNames() {
        return new ArrayList<>(assetPackNames);
    }

    public static void setActiveAssetPackNames(ArrayList<String> assetPackNames) {
        if (AssetManager.assetPackNames.equals(assetPackNames)) return;
        AssetManager.assetPackNames.clear();
        AssetManager.assetPackNames.addAll(assetPackNames);
        deleteAll();
    }

    private static void addAssetFilePathsInFolder(ArrayList<Path> filePaths, Path folderPath) {
        File[] files = FileManager.getChildren(folderPath);
        for (File file : files) filePaths.add(file.toPath());
    }

    private static final ArrayList<Runnable> deleteAllCallbacks = new ArrayList<>();
    private static final HashMap<AssetIdentifier<?>, Asset> assets = new HashMap<>();
    private static final ArrayList<String> assetPackNames = new ArrayList<>();
}
