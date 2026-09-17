package game.player.movement;

import core.rendering_api.Input;
import core.utils.MathUtils;

import game.assets.Model;
import game.player.rendering.Camera;
import game.server.Game;
import game.settings.KeySettings;
import game.settings.OptionSettings;
import core.utils.MainThread;
import game.utils.Position;

import game.utils.ServerThread;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.assets.Models.*;
import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class SwimmingState extends MovementState {

    @Override
    @ServerThread
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySettings.SPRINT) || !Input.isKeyPressed(KeySettings.MOVE_FORWARD)) next = MovementState.load(CrawlingState.class);

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = MathUtils.getDirection(playerRotation);

        float liquidVolume = intersectedVolume(lastPosition, this, WATER) + intersectedVolume(lastPosition, this, LAVA);
        float airVolume = Math.min(intersectedVolume(lastPosition, this, AIR), 50.0F);

        velocityChange.set(playerDirection).mul(liquidVolume * swimSpeed * 0.5F + airVolume * swimSpeed);

        return velocityChange;
    }

    @Override
    @ServerThread
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        if (!intersectsLiquid(playerPosition, this)) next = MovementState.load(CrawlingState.class);

        float waterIntersection = intersectedVolume(playerPosition, this, WATER);
        float lavaIntersection = intersectedVolume(playerPosition, this, LAVA);

        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));
        velocity.add(acceleration).mul(liquidDrag * AIR_DRAG);

        if (velocity.y > 0.0F) {
            Position nextPosition = new Position(playerPosition);
            nextPosition.addComponent(Y_COMPONENT, velocity.y);
            if (!intersectsLiquid(nextPosition, this)) velocity.y = 0.0F;
        }
    }

    @Override
    @MainThread
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
        float fraction = Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0, 1);
        Vector3f velocity = movement.getRenderVelocity().mul(fraction).add(movement.getOldRenderVelocity().mul(1 - fraction));

        applyBasicAnimation(camera.getRotation(), velocity, transforms, playerCharacter.boxes());

        float rotationSpeed = camera.getCurrentRotationSpeed(fraction).length() * 0.05F;
        float speed = velocity.length();
        float amplitude = (float) Math.clamp(speed + rotationSpeed, -Math.PI * 0.35, Math.PI * 0.35);
        double swimAngle = Math.abs(Math.sin(animationTimer) * Math.PI);
        double liftAngle = Math.max(0, Math.sin(animationTimer * 2) * Math.PI * 0.35);

        transforms[HEAD].translate(0, -22, -6).rotate((float) -Math.PI * 0.25F, 1, 0, 0);
        transforms[BODY].translate(0, -10, 6).rotate((float) -Math.PI * 0.5F, 1, 0, 0);
        transforms[LEFT_ARM].translate(0, -20, -4).rotate((float) -Math.PI * 0.5F, 1, 0, 0)
                .rotate((float) -swimAngle, 0, 0, 1)
                .rotate((float) liftAngle, 1, 0, 0);
        transforms[RIGHT_ARM].translate(0, -20, -4).rotate((float) -Math.PI * 0.5F, 1, 0, 0)
                .rotate((float) swimAngle, 0, 0, 1)
                .rotate((float) liftAngle, 1, 0, 0);
        applyLegAnimation(animationTimer, transforms, amplitude);

        return animationTimer + frameTime * amplitude * 0.0025;
    }

    @MainThread
    static void applyBasicAnimation(Vector3f cameraRotation, Vector3f velocity, Matrix4f[] transforms, Model.ModelBox[] boxes) {
        Vector3f direction = MathUtils.getHorizontalDirection(cameraRotation);
        float angle = (float) -Math.toRadians(cameraRotation.y);
        float sidewaysVelocity = -velocity.x * direction.z + velocity.z * direction.x;
        float sidewaysTilt = (float) Math.clamp(sidewaysVelocity * 0.2F, -Math.PI * 0.25, Math.PI * 0.25);
        boolean isFirstPerson = OptionSettings.PERSPECTIVE.value() == Camera.Perspective.FIRST_PERSON;

        for (int index = BODY; index < transforms.length; index++)
            transforms[index].identity()
                    .rotate(angle - sidewaysTilt, 0, 1, 0)
                    .translate(boxes[index].position())
                    .translate(0, 0, isFirstPerson ? 3 : 0);
        if (isFirstPerson)
            transforms[HEAD].identity()
                    .rotate(angle, 0, 1, 0)
                    .translate(boxes[HEAD].position());
        else transforms[HEAD].identity()
                .rotate(angle, 0, 1, 0)
                .translate(boxes[HEAD].position())
                .rotate(-sidewaysTilt, 0, 1, 0)
                .translate(0, 0, -6)
                .rotate(sidewaysTilt, 0, 1, 0)
                .translate(0, 0, 6);
    }

    @MainThread
    static void applyLegAnimation(double animationTimer, Matrix4f[] transforms, float amplitude) {
        transforms[LEFT_LEG].translate(0, -10, 6).rotate((float) -Math.PI * 0.5F, 1, 0, 0)
                .rotate((float) -Math.sin(animationTimer * 2) * amplitude * 0.35F, 1, 0, 0);
        transforms[RIGHT_LEG].translate(0, -10, 6).rotate((float) -Math.PI * 0.5F, 1, 0, 0)
                .rotate((float) Math.sin(animationTimer * 2) * amplitude * 0.35F, 1, 0, 0);
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
