package game.player.rendering;

import core.rendering_api.Window;
import core.settings.optionSettings.Option;
import core.utils.MathUtils;
import core.utils.Vector3l;

import game.server.Game;
import game.server.World;
import game.server.material.Properties;
import game.settings.FloatSettings;
import game.settings.OptionSettings;
import game.utils.Position;

import org.joml.*;

import java.lang.Math;

import static game.utils.Constants.*;

public final class Camera {

    public Camera() {
        position = new Position(new Vector3l(), new Vector3f());
        rotation = new Vector3f(0.0F, 0.0F, 0.0F);
        updateProjectionMatrix();
    }


    public void updateProjectionMatrix() {
        projectionMatrix
                .identity()
                .setPerspective((float) Math.toRadians(FloatSettings.FOV.value() * zoomFactor), Window.getAspectRatio(), Z_FAR, Z_NEAR, true);
    }

    public void setPosition(Position position) {
        synchronized (this) {
            this.position = new Position(position);
        }
    }

    public void rotate(Vector2i cursorMovement) {
        float sensitivityFactor = FloatSettings.SENSITIVITY.value() * 0.6F + 0.2F;
        sensitivityFactor = 1.2F * sensitivityFactor * sensitivityFactor * sensitivityFactor;
        float rotationYaw = cursorMovement.x * sensitivityFactor;
        float rotationPitch = cursorMovement.y * sensitivityFactor;

        moveRotation(rotationYaw, -rotationPitch);
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

    public Vector3f getDirection() {
        synchronized (this) {
            return MathUtils.getDirection(rotation);
        }
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Position getPosition() {
        synchronized (this) {
            return new Position(position);
        }
    }

    public Vector3f getRotation() {
        synchronized (this) {
            return new Vector3f(rotation);
        }
    }

    public int getPrimaryDirection() {
        Vector3f direction = getDirection();
        int component = direction.maxComponent();

        if (component == X_COMPONENT) return direction.x > 0.0F ? WEST : EAST;
        if (component == Y_COMPONENT) return direction.y > 0.0F ? TOP : BOTTOM;
        if (component == Z_COMPONENT) return direction.z > 0.0F ? NORTH : SOUTH;
        throw new IllegalStateException(component + " is not a valid Component");
    }


    public Position applyPerspectiveOffset(Position position) {
        if (OptionSettings.PERSPECTIVE.value() == Perspective.FIRST_PERSON) return position;
        Vector3f direction = getDirection().mul(OptionSettings.PERSPECTIVE.value() == Perspective.SECOND_PERSON ? 1 : -1);
        int length = 0;

        while (length++ < PERSPECTIVE_OFFSET_LENGTH && !isObstructed(position)) position.add(direction.x, direction.y, direction.z);
        if (length < PERSPECTIVE_OFFSET_LENGTH) pushPositionToWall(position, direction);

        return position;
    }


    private void moveRotation(float yaw, float pitch) {
        synchronized (this) {
            rotation.x += pitch;
            rotation.y += yaw;

            rotation.x = Math.clamp(rotation.x, -90.0F, 90.0F);
            rotation.y %= 360.0F;
        }
    }

    private static boolean isObstructed(Position position) {
        long startX = position.longX - 2, endX = position.longX + 2;
        long startY = position.longY - 2, endY = position.longY + 2;
        long startZ = position.longZ - 2, endZ = position.longZ + 2;
        World world = Game.getWorld();

        for (long x = startX; x <= endX; x++)
            for (long y = startY; y <= endY; y++)
                for (long z = startZ; z <= endZ; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, TRANSPARENT)) return true;
                }
        return false;
    }

    private static void pushPositionToWall(Position position, Vector3f direction) {
        direction.mul(-0.05F);
        while (isObstructed(position)) position.add(direction.x, direction.y, direction.z);
    }


    private boolean zoomed = false;
    private float zoomFactor = 1.0F;
    private Position position;
    private final Vector3f rotation;

    private final Matrix4f projectionMatrix = new Matrix4f();

    public enum Perspective implements Option {
        FIRST_PERSON, THIRD_PERSON, SECOND_PERSON
    }
}
