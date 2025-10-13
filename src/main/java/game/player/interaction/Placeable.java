package game.player.interaction;

import game.server.Chunk;
import game.server.generation.Structure;

import org.joml.Vector3i;

import java.util.ArrayList;

public interface Placeable {

    void place(Vector3i position, int lod);

    ArrayList<Chunk> getAffectedChunks();

    Structure getStructure();

}
