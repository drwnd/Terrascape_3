package game.assets;

import core.assets.identifiers.AssetIdentifier;
import game.server.generation.Structure;
import game.server.saving.StructureSaver;

public record StructureIdentifier(String structureName) implements AssetIdentifier<Structure> {
    @Override
    public Structure generateAsset() {
        return new StructureSaver().load(StructureSaver.getSaveFileLocation(structureName));
    }
}
