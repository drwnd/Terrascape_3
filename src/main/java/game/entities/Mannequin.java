package game.entities;

import game.assets.EntityType;
import game.entities.movement.EntityMovementState;
import game.entities.movement.MovementState;
import game.utils.Position;

import org.joml.Vector3f;

public class Mannequin extends Entity {

    public Mannequin(Position position) {
        super(position, new RenderEntity(EntityType.PLAYER, new PlayerAnimator()), MovementState.load(MannequinMovementState.class));
    }

    private static class MannequinMovementState extends EntityMovementState {

        @Override
        protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
            return new Vector3f();
        }
    }
}
