package game.assets;

import com.google.gson.Gson;

import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.utils.FileManager;
import core.utils.MainThread;
import game.entities.*;

import java.nio.file.Path;

public enum Models implements AssetIdentifier<Model> {
    PLAYER(new PlayerAnimator()),
    ZOMBIE(new Zombie.ZombieAnimator());

    @Override
    @MainThread
    public Model generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("models", name() + ".json"));
        ModelData modelData = new Gson().fromJson(FileManager.loadJson(filepath), ModelData.class);
        return new Model(modelData, animator);
    }


    Models(Animator animator) {
        this.animator = animator;
    }

    private final Animator animator;
}
