package game.assets;

import com.google.gson.Gson;

import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.utils.FileManager;

import java.nio.file.Path;

public enum Models implements AssetIdentifier<Model> {
    PLAYER_MODEL;

    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;

    @Override
    public Model generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("models", name() + ".json"));
        Model.ModelData modelData = new Gson().fromJson(FileManager.loadJson(filepath), Model.ModelData.class);
        return new Model(modelData);
    }
}
