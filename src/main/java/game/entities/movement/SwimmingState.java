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
import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class SwimmingState extends MovementState {

    @Override
    @ServerThread
    protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySettings.SPRINT) || !Input.isKeyPressed(KeySettings.MOVE_FORWARD)) next = MovementState.load(CrawlingState.class);

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = MathUtils.getDirection(rotation);

        float liquidVolume = intersectedVolume(lastPosition, this, WATER) + intersectedVolume(lastPosition, this, LAVA);
        float airVolume = Math.min(intersectedVolume(lastPosition, this, AIR), 50.0F);

        velocityChange.set(playerDirection).mul(liquidVolume * swimSpeed * 0.5F + airVolume * swimSpeed);

        return velocityChange;
    }

    @Override
    @ServerThread
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position position, Vector3f rotation) {
        if (!intersectsLiquid(position, this)) next = MovementState.load(CrawlingState.class);

        float waterIntersection = intersectedVolume(position, this, WATER);
        float lavaIntersection = intersectedVolume(position, this, LAVA);

        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));
        velocity.add(acceleration).mul(liquidDrag * AIR_DRAG);

        if (velocity.y > 0.0F) {
            Position nextPosition = new Position(position);
            nextPosition.addComponent(Y_COMPONENT, velocity.y);
            if (!intersectsLiquid(nextPosition, this)) velocity.y = 0.0F;
        }
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
        double swimAngle = Math.abs(Math.sin(animationTimer) * Math.PI);
        double liftAngle = Math.max(0, Math.sin(animationTimer * 2) * Math.PI * 0.35);

        transforms[HEAD].rotate((float) -Math.PI * 0.25F, 1, 0, 0);
        transforms[BODY].translate(0, -10, 6).rotate((float) -Math.PI * 0.5F, 1, 0, 0);
        transforms[LEFT_ARM]
                .rotate((float) -swimAngle, 0, 0, 1)
                .rotate((float) liftAngle, 1, 0, 0);
        transforms[RIGHT_ARM]
                .rotate((float) swimAngle, 0, 0, 1)
                .rotate((float) liftAngle, 1, 0, 0);
        applyLegAnimation(animationTimer, transforms, amplitude);

        playerCharacter.animationTimer += frameTime * amplitude * 0.0025;
    }

    @MainThread
    static void applyLegAnimation(double animationTimer, Matrix4f[] transforms, float amplitude) {
        transforms[LEFT_LEG].rotate((float) -Math.sin(animationTimer * 2) * amplitude * 0.35F, 1, 0, 0);
        transforms[RIGHT_LEG].rotate((float) Math.sin(animationTimer * 2) * amplitude * 0.35F, 1, 0, 0);
    }

    @Override
    public byte getIdentifier() {
        return 1;
    }

    @Override
    byte getStandingMaterial(Position position) {
        return WATER;
    }

    @SuppressWarnings("unused")
    private float swimSpeed;
}
