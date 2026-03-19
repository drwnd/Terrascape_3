package game.player.interaction;

import core.utils.Vector3l;

import game.server.Chunk;
import game.server.generation.Structure;

import java.util.ArrayList;

public interface Placeable {

    void place(Vector3l position, int lod);

    ArrayList<Chunk> getAffectedChunks();

    Structure getStructure();

    boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    void offsetPosition(Vector3l position);

    void spawnParticles(Vector3l position);
}
