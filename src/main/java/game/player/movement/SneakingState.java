package game.player.movement;

import core.rendering_api.Input;
import core.utils.MathUtils;

import game.assets.Model;
import game.player.rendering.Camera;
import game.server.Game;
import game.settings.KeySettings;
import core.utils.MainThread;
import game.utils.Position;

import game.utils.ServerThread;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.assets.Models.*;
import static org.lwjgl.glfw.GLFW.*;

public final class SneakingState extends MovementState {

    @Override
    @ServerThread
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySettings.SNEAK)) next = MovementState.load(WalkingState.class);
        if (Input.isKeyPressed(KeySettings.CRAWL)) next = MovementState.load(CrawlingState.class);
        if (Input.isKeyPressed(KeySettings.SPRINT) && Input.isKeyPressed(KeySettings.MOVE_FORWARD) && intersectsLiquid(lastPosition, this))
            next = MovementState.load(CrawlingState.class);

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = MathUtils.getHorizontalDirection(playerRotation);
        float speed = getMovementSpeed(lastPosition, sneakingSpeed, inAirSpeed, swimStrength);

        applyXZMovement(velocityChange, speed, 1.0F);

        if (Input.isKeyPressed(KeySettings.JUMP)) handleJump(lastPosition, velocityChange, jumpStrength, swimStrength);

        normalizeXZToMaxComponent(velocityChange);
        toWorldDirection(velocityChange, playerDirection);

        return velocityChange;
    }

    @Override
    @ServerThread
    void handleInput(int key, int action) {
        if (key == KeySettings.JUMP.keybind() && action == GLFW_PRESS) {
            if (System.nanoTime() - lastJumpTime < JUMP_FLYING_INTERVALL) next = MovementState.load(FlyingState.class);
            lastJumpTime = System.nanoTime();
        }
    }

    @Override
    @MainThread
    public double applyAnimation(Model playerCharacter, Camera camera, double animationTimer, float frameTime) {
        Matrix4f[] transforms = playerCharacter.transforms();
        Model.ModelBox[] boxes = playerCharacter.boxes();
        float fraction = Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0, 1);
        Vector3f velocity = movement.getRenderVelocity().mul(fraction).add(movement.getOldRenderVelocity().mul(1 - fraction));
        Vector3f cameraRotation = camera.getRotation();

        WalkingState.rotateBody(transforms, boxes, cameraRotation, velocity);

        float rotationSpeed = camera.getCurrentRotationSpeed(fraction).length() * 0.05F;
        float speed = (float) Math.clamp(velocity.length() * 0.2 + rotationSpeed, -Math.PI * 0.5, Math.PI * 0.5);
        transforms[HEAD].translate(0, -4, 0).rotate((float) -Math.toRadians(cameraRotation.x), 1, 0, 0);
        transforms[BODY].translate(0, -2, 6).rotate(-0.5F, 1, 0, 0);
        transforms[LEFT_ARM].translate(0, -4, 0).rotate((float) Math.sin(animationTimer) * speed, 1, 0, 0).rotate(-speed * 0.2F, 0, 0, 1);
        transforms[RIGHT_ARM].translate(0, -4, 0).rotate((float) -Math.sin(animationTimer) * speed, 1, 0, 0).rotate(speed * 0.2F, 0, 0, 1);
        transforms[LEFT_LEG].translate(0, 0, 5.5F).rotate((float) -Math.sin(animationTimer) * speed * 0.5F, 1, 0, 0);
        transforms[RIGHT_LEG].translate(0, 0, 5.5F).rotate((float) Math.sin(animationTimer) * speed * 0.5F, 1, 0, 0);

        return animationTimer + frameTime * 0.005;
    }

    @Override
    public byte getIdentifier() {
        return 2;
    }

    @SuppressWarnings("unused")
    private float jumpStrength, swimStrength, sneakingSpeed, inAirSpeed;
}
