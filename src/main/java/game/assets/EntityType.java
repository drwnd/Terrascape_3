package game.assets;

import com.google.gson.Gson;

import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.utils.FileManager;
import core.utils.MainThread;

import game.entities.*;
import game.utils.Position;

import java.nio.file.Path;

public enum EntityType implements AssetIdentifier<Model> {
    PLAYER(Mannequin::new),
    ZOMBIE(Zombie::new),
    SKELETON(Skeleton::new);

    @Override
    @MainThread
    public Model generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("models", name() + ".json"));
        ModelData modelData = new Gson().fromJson(FileManager.loadJson(filepath), ModelData.class);
        return new Model(modelData);
    }


    EntityType(EntityBuilder entityBuilder) {
        this.entityBuilder = entityBuilder;
    }

    public Entity getEntity(Position position) {
        return entityBuilder.getEntity(position);
    }

    private final EntityBuilder entityBuilder;

    private interface EntityBuilder {
        Entity getEntity(Position position);
    }
}
