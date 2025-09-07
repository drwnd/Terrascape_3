package player.movement;

import org.joml.Vector3f;
import server.Game;
import utils.Position;

public final class Movement {

    public Movement(Vector3f initialVelocity) {
        velocity = initialVelocity;
    }

    public Position computeNextGameTickPosition(Position lastPosition, Vector3f rotation) {
        Position position = new Position(lastPosition);
        move(position);

        Vector3f acceleration = Game.getPlayer().canDoActiveActions() ? state.computeNextGameTickAcceleration(rotation, lastPosition) : new Vector3f(0.0f);
        state.changeVelocity(velocity, acceleration, lastPosition, rotation);

        state = state.next;
        return position;
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public static void normalizeToMaxComponent(Vector3f velocity) {
        float max = Math.abs(velocity.get(velocity.maxComponent()));
        if (max < 1E-4) return;
        velocity.normalize(max);
    }

    public void handleInput(int button, int action) {
        state.handleInput(button, action);
    }

    private void move(Position position) {
        position.add(velocity.x, velocity.y, velocity.z);
    }

    private MovementState state = new FlyingState();
    private final Vector3f velocity;
}
