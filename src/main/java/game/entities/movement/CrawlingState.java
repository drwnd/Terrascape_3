package game.entities.movement;

import core.rendering_api.Input;
import core.utils.MathUtils;
import core.utils.MainThread;

import game.entities.RenderEntity;
import game.player.rendering.Camera;
import game.settings.KeySettings;
import game.utils.Position;
import game.utils.ServerThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.entities.PlayerAnimator.*;
import static org.lwjgl.glfw.GLFW.*;

public final class CrawlingState extends MovementState {

    @Override
    @ServerThread
    protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySettings.CRAWL)) next = MovementState.load(SneakingState.class);
        if (Input.isKeyPressed(KeySettings.SPRINT) && Input.isKeyPressed(KeySettings.MOVE_FORWARD) && intersectsLiquid(lastPosition, this))
            next = MovementState.load(SwimmingState.class);

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = MathUtils.getHorizontalDirection(rotation);
        float speed = getMovementSpeed(lastPosition, crawlingSpeed, inAirSpeed, swimStrength);

        applyXZMovement(velocityChange, speed, 1.0F);

        if (Input.isKeyPressed(KeySettings.JUMP)) handleJump(lastPosition, velocityChange, jumpStrength, swimStrength);

        normalizeXZToMaxComponent(velocityChange);
        toWorldDirection(velocityChange, playerDirection);

        return velocityChange;
    }

    @Override
    @MainThread
    protected void handleInput(int key, int action) {
        if (key == KeySettings.JUMP.keybind() && action == GLFW_PRESS) {
            if (System.nanoTime() - lastJumpTime < JUMP_FLYING_INTERVALL) next = MovementState.load(FlyingState.class);
            lastJumpTime = System.nanoTime();
        }
    }

    @Override
    @MainThread
    public void applyAnimation(RenderEntity playerCharacter, Camera camera, float frameTime, float gameTickFraction, Vector3f velocity) {
        Matrix4f[] transforms = playerCharacter.transforms();

        float rotationSpeed = camera.getCurrentRotationSpeed(gameTickFraction).length() * 0.05F;
        float speed = velocity.length();
        float amplitude = (float) Math.clamp(speed + rotationSpeed, -Math.PI * 0.35, Math.PI * 0.35);
        double animationTimer = playerCharacter.animationTimer;

        transforms[HEAD].rotate((float) -Math.PI * 0.25F, 1, 0, 0);
        transforms[BODY].translate(0, -10, 6).rotate((float) -Math.PI * 0.5F, 1, 0, 0);
        transforms[LEFT_ARM]
                .rotate((float) -Math.abs(Math.sin(animationTimer) * Math.PI), 0, 0, 1)
                .rotate((float) Math.min(0, -Math.sin(animationTimer * 2) * Math.PI * 0.25), 1, 0, 0);
        transforms[RIGHT_ARM]
                .rotate((float) Math.abs(Math.cos(animationTimer) * Math.PI), 0, 0, 1)
                .rotate((float) Math.min(0, Math.sin(animationTimer * 2) * Math.PI * 0.25), 1, 0, 0);
        SwimmingState.applyLegAnimation(animationTimer, transforms, amplitude);

        playerCharacter.animationTimer += frameTime * amplitude * 0.0025;
    }

    @Override
    public byte getIdentifier() {
        return 4;
    }

    @SuppressWarnings("unused")
    private float jumpStrength, swimStrength, crawlingSpeed, inAirSpeed;
}
