package game.entities.model;

import game.server.generation.Structure;

import org.joml.Vector3f;

public record CustomModelStructure(Vector3f center, Vector3f position, Structure structure, Integer parentIndex,
                            boolean ignoreParentRotation) implements StructurePart {
}
