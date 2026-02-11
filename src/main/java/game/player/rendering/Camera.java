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
        rotation = new Vector3f(0.0F, 0.0F, 0.0F);
        updateProjectionMatrix();
    }

    public void updateProjectionMatrix() {
        projectionMatrix
                .identity()
                .setPerspective((float) Math.toRadians(FloatSetting.FOV.value() * zoomFactor), Window.getAspectRatio(), Z_FAR, Z_NEAR, true);
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

            rotation.x = Math.max(-90.0F, Math.min(rotation.x, 90.0F));
            rotation.y %= 360.0F;
        }
    }

    public void rotate(Vector2i cursorMovement) {
        float sensitivityFactor = FloatSetting.SENSITIVITY.value() * 0.6F + 0.2F;
        sensitivityFactor = 1.2F * sensitivityFactor * sensitivityFactor * sensitivityFactor;
        float rotationYaw = cursorMovement.x * sensitivityFactor;
        float rotationPitch = cursorMovement.y * sensitivityFactor;

        moveRotation(rotationYaw, -rotationPitch);
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

    public void setZoomed(boolean zoomed) {
        this.zoomed = zoomed;
        if (zoomed) zoomFactor = 0.25F;
        else zoomFactor = 1.0F;
    }

    public void changeZoom(float multiplier) {
        zoomFactor *= multiplier;
    }

    public boolean isZoomed() {
        return zoomed;
    }

    private boolean zoomed = false;
    private float zoomFactor = 1.0F;
    private Position position;
    private final Vector3f rotation;

    private final Matrix4f projectionMatrix = new Matrix4f();
}
