package game.assets;

import core.assets.AssetGenerator;
import core.assets.AssetManager;
import core.assets.identifiers.AssetIdentifier;
import core.utils.FileManager;
import game.server.generation.Structure;
import game.server.saving.StructureSaver;

import java.io.File;
import java.util.ArrayList;

public enum StructureCollectionIdentifier implements AssetIdentifier<StructureCollection> {
    OAK_TREES("OakTree"),
    SPRUCE_TREES("SpruceTree"),
    DARK_OAK_TREES("DarkOakTree"),
    PINE_TREES("PineTree"),
    REDWOOD_TREES("RedwoodTree"),
    BLACK_WOOD_TREES("BlackWoodTree");

    StructureCollectionIdentifier(String structureBaseName) {
        this.structureBaseName = structureBaseName.toLowerCase();
    }

    @Override
    public AssetGenerator<StructureCollection> getAssetGenerator() {
        return () -> {
            File[] structureFiles = FileManager.getChildren(new File(StructureSaver.getSaveFileLocation()));
            ArrayList<Structure> structuresList = new ArrayList<>();

            for (File structureFile : structureFiles) {
                String structureName = structureFile.getName().toLowerCase();
                if (!structureName.startsWith(structureBaseName)) continue;
                Structure structure = AssetManager.get(new StructureIdentifier(structureName));
                structuresList.add(structure);
            }

            Structure[] structures = new Structure[structuresList.size()];
            for (int index = 0; index < structures.length; index++)
                structures[index] = structuresList.get(index);

            return new StructureCollection(structures);
        };
    }

    private final String structureBaseName;
}
