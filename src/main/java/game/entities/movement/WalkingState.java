package game.entities.movement;

import core.rendering_api.Input;
import core.sound.Sound;
import core.utils.MathUtils;
import core.utils.MainThread;

import game.entities.RenderEntity;
import game.player.rendering.Camera;
import game.server.material.Material;
import game.settings.FloatSettings;
import game.settings.KeySettings;
import game.utils.Position;
import game.utils.ServerThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.entities.PlayerAnimator.*;
import static org.lwjgl.glfw.GLFW.*;

public final class WalkingState extends MovementState {

    @Override
    @ServerThread
    protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
        if (Input.isKeyPressed(KeySettings.SNEAK)) next = MovementState.load(SneakingState.class);
        if (Input.isKeyPressed(KeySettings.CRAWL)) next = MovementState.load(CrawlingState.class);
        if (Input.isKeyPressed(KeySettings.SPRINT) && Input.isKeyPressed(KeySettings.MOVE_FORWARD) && intersectsLiquid(lastPosition, this))
            next = MovementState.load(SneakingState.class);

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = MathUtils.getHorizontalDirection(rotation);
        float speed = getMovementSpeed(lastPosition, walkingSpeed, inAirSpeed, swimStrength);

        applyXZMovement(velocityChange, speed, sprintSpeedModifier);

        if (Input.isKeyPressed(KeySettings.JUMP)) {
            playJumpSound(lastPosition);
            handleJump(lastPosition, velocityChange, jumpStrength, swimStrength);
            if (Input.isKeyPressed(KeySettings.MOVE_FORWARD) && Input.isKeyPressed(KeySettings.SPRINT) && movement.isGrounded())
                velocityChange.x += jumpSpeedGain;
        }

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
    public byte getIdentifier() {
        return 0;
    }

    @Override
    @ServerThread
    public int ticksBetweenFootsteps() {
        return Input.isKeyPressed(KeySettings.SPRINT) ? ticksBetweenFootstepsWhenSprinting : super.ticksBetweenFootsteps();
    }

    @Override
    @MainThread
    public void applyAnimation(RenderEntity playerCharacter, Camera camera, float frameTime, float gameTickFraction, Vector3f velocity) {
        Matrix4f[] transforms = playerCharacter.transforms();
        Vector3f cameraRotation = camera.getRotation();

        double animationTimer = playerCharacter.animationTimer;
        float rotationSpeed = camera.getCurrentRotationSpeed(gameTickFraction).length() * 0.05F;
        float speed = (float) Math.clamp(velocity.length() * 0.2 + rotationSpeed, -Math.PI * 0.5, Math.PI * 0.5);
        float bodyMovement = (float) Math.cos(animationTimer) * Math.min(speed * 0.1F, 0.3F);

        transforms[HEAD].translate(0, bodyMovement, 0).rotate((float) -Math.toRadians(cameraRotation.x), 1, 0, 0);
        transforms[BODY].translate(0, bodyMovement, 0);
        transforms[LEFT_ARM].rotate((float) Math.sin(animationTimer) * speed, 1, 0, 0).rotate(-speed * 0.1F, 0, 0, 1);
        transforms[RIGHT_ARM].rotate((float) -Math.sin(animationTimer) * speed, 1, 0, 0).rotate(speed * 0.1F, 0, 0, 1);
        transforms[LEFT_LEG].rotate((float) -Math.sin(animationTimer) * speed * 0.5F, 1, 0, 0);
        transforms[RIGHT_LEG].rotate((float) Math.sin(animationTimer) * speed * 0.5F, 1, 0, 0);

        playerCharacter.animationTimer += (Input.isKeyPressed(KeySettings.SPRINT) ? 1.5 : 1) * frameTime * 0.01;
    }

    @ServerThread
    private void playJumpSound(Position position) {
        if (!movement.isGrounded()) return;
        byte standingMaterial = getStandingMaterial(position);
        Sound.play3D(Material.getJumpSounds(standingMaterial), FloatSettings.JUMP_AUDIO, position, null);
    }

    @SuppressWarnings("unused")
    private float jumpStrength, swimStrength, walkingSpeed, inAirSpeed, sprintSpeedModifier, jumpSpeedGain;
    @SuppressWarnings("unused")
    private int ticksBetweenFootstepsWhenSprinting;
}
