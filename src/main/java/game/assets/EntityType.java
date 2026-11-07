package game.assets;

import com.google.gson.Gson;

import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.language.Translatable;
import core.utils.FileManager;
import core.utils.MainThread;

import game.entities.*;
import game.entities.model.Model;
import game.entities.model.ModelData;
import game.utils.Position;

import java.nio.file.Path;

public enum EntityType implements AssetIdentifier<Model>, Translatable {
    PLAYER(Mannequin::new, "Armor Stand"),
    ZOMBIE(Zombie::new, "Zombie Head"),
    SKELETON(Skeleton::new, "Skeleton Head"),
    MANNEQUIN(Mannequin::new, "Armor Stand");

    @Override
    @MainThread
    public Model generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("models", name() + ".json"));
        ModelData modelData = new Gson().fromJson(FileManager.loadJson(filepath), ModelData.class);
        return new Model(modelData);
    }


    EntityType(EntityBuilder entityBuilder, String structureName) {
        this.entityBuilder = entityBuilder;
        this.structureIdentifier = new StructureIdentifier(structureName);
    }

    public Entity getEntity(Position position) {
        return entityBuilder.getEntity(position);
    }

    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    private final EntityBuilder entityBuilder;
    private final StructureIdentifier structureIdentifier;

    @Override
    public String translationFileName() {
        return "entities";
    }

    private interface EntityBuilder {
        Entity getEntity(Position position);
    }
}
