package game.entities.movement;

import game.entities.RenderEntity;
import game.player.rendering.Camera;
import org.joml.Vector3f;

public abstract class EntityMovementState extends MovementState {

    protected long lastActionTick = 0;

    @Override
    public boolean isNoClip() {
        return false;
    }

    @Override
    public void applyAnimation(RenderEntity playerCharacter, Camera camera, float frameTime, float gameTickFraction, Vector3f velocity) {
        throw new UnsupportedOperationException("Only for Player movement");
    }

    @Override
    protected void handleInput(int key, int action) {
        throw new UnsupportedOperationException("Only for Player movement");
    }

    @Override
    public byte getIdentifier() {
        throw new UnsupportedOperationException("Only for Player movement");
    }

    protected float walkingSpeed;
}
