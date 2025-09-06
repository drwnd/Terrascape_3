package player.movement;

import org.joml.Vector3f;
import utils.Position;

public abstract class MovementState {

    MovementState next = this;

    /**
     * Computes the acceleration the Player should have in the next Gametick.
     * Only gets called when the Player is actively moving.
     *
     * @param playerRotation The rotation of the player (not necessarily the camera)
     * @param lastPositon    The position of the player in the last Gametick
     *
     * @return The acceleration of the Player.
     */
    abstract Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPositon);

    /**
     * Changes the current Velocity the Player has.
     * Gets called every Gametick regardless of the Player being able to move actively.
     * Therefore, no inputs should be queried in this method.
     *
     * @param velocity The current Velocity of the Player. This is also the output variable.
     * @param acceleration The acceleration computed in computeNextGameTickAcceleration(...) or 0 if the Player can't move actively.
     * @param playerPosition The position of the Player.
     * @param playerRotation The rotation of the Player.
     */
    abstract void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation);

    abstract void handleInput(int key, int action);
}
