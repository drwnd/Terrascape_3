package game.assets;

import core.assets.AssetGenerator;
import core.assets.identifiers.AssetIdentifier;
import game.server.generation.Structure;
import game.server.saving.StructureSaver;

public record StructureIdentifier(String structureName) implements AssetIdentifier<Structure> {
    @Override
    public AssetGenerator<Structure> getAssetGenerator() {
        return () -> new StructureSaver().load(StructureSaver.getSaveFileLocation(structureName));
    }
}
