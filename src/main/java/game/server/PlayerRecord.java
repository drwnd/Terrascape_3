package game.server;

import game.utils.Position;
import org.joml.Vector3f;

import java.util.ArrayList;

public record PlayerRecord (ArrayList<Position> positions, ArrayList<Vector3f> rotations) {
}
