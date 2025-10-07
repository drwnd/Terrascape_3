package game.player.rendering;

import core.rendering_api.Window;
import core.settings.FloatSetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.*;

import java.lang.Math;

import static game.utils.Constants.Z_FAR;
import static game.utils.Constants.Z_NEAR;

public final class Camera {

    public Camera() {
        position = new Position(new Vector3i(), new Vector3f());
        rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        updateProjectionMatrix();
    }

    public void updateProjectionMatrix() {
        projectionMatrix.setPerspective((float) Math.toRadians(FloatSetting.FOV.value()), Window.getAspectRatio(), Z_NEAR, Z_FAR);
    }

    public Vector3f getDirection() {
        synchronized (this) {
            return Utils.getDirection(rotation);
        }
    }

    private void moveRotation(float yaw, float pitch) {
        synchronized (this) {
            rotation.x += pitch;
            rotation.y += yaw;

            rotation.x = Math.max(-90, Math.min(rotation.x, 90));
            rotation.y %= 360.0f;
        }
    }

    public void rotate(Vector2i cursorMovement) {
        float sensitivityFactor = FloatSetting.SENSITIVITY.value() * 0.6f + 0.2f;
        sensitivityFactor = 1.2f * sensitivityFactor * sensitivityFactor * sensitivityFactor;
        float rotationX = cursorMovement.x * sensitivityFactor;
        float rotationY = cursorMovement.y * sensitivityFactor;

        moveRotation(rotationX, -rotationY);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Position getPosition() {
        synchronized (this) {
            return new Position(position);
        }
    }

    public void setPosition(Position position) {
        synchronized (this) {
            this.position = new Position(position);
        }
    }

    public Vector3f getRotation() {
        synchronized (this) {
            return new Vector3f(rotation);
        }
    }

    public void setRotation(Vector3f rotation) {
        synchronized (this) {
            this.rotation.set(rotation);
        }
    }

    private Position position;
    private final Vector3f rotation;

    private final Matrix4f projectionMatrix = new Matrix4f();
}
