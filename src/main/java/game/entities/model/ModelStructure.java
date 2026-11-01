package game.entities.model;

import core.assets.AssetManager;

import game.assets.StructureIdentifier;
import game.server.generation.Structure;

import org.joml.Vector3f;

public record ModelStructure(Vector3f center, Vector3f position, String structureName, Integer parentIndex, boolean ignoreParentRotation) implements StructurePart {
    @Override
    public Structure structure() {
        return AssetManager.get(new StructureIdentifier(structureName));
    }
}
