package game.entities;

import game.assets.EntityType;
import game.entities.movement.EntityMovementState;
import game.player.rendering.Skin;
import game.utils.Position;

import org.joml.Vector3f;

public class Mannequin extends Entity {

    public Mannequin(Position position) {
        super(position, new RenderEntity(EntityType.PLAYER, Skin.getRandomSkin(), new PlayerAnimator()), MannequinMovementState.class, EntityType.MANNEQUIN);
    }

    @Override
    public int getArmTransformIndex() {
        return PlayerAnimator.RIGHT_ARM;
    }

    @Override
    public Vector3f getHandPosition() {
        return new Vector3f(0, -8, 0);
    }

    private static class MannequinMovementState extends EntityMovementState {

        @Override
        protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
            return new Vector3f();
        }
    }
}
