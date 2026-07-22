package game.utils;

import core.utils.MathUtils;
import game.player.rendering.Camera;

import game.server.generation.Structure;
import game.settings.OptionSettings;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.utils.Constants.*;

public final class Transformation {

    /**
     * Creates a combined projection and rotation matrix for the specified camera.
     * Takes into account the camera's perspective mode.
     *
     * @param camera the camera used to determine the projection and rotation
     * @return a Matrix4f representing the projection multiplied by rotation
     */
    public static Matrix4f createProjectionRotationMatrix(Camera camera) {
        Vector3f rotation = camera.getRotation();
        if (OptionSettings.PERSPECTIVE.value() == Camera.Perspective.SECOND_PERSON) {
            rotation.x *= -1;
            rotation.y += 180;
        }

        Matrix4f matrix = new Matrix4f(camera.getProjectionMatrix());
        matrix.rotate((float) Math.toRadians(rotation.x), 1.0F, 0.0F, 0.0F)
                .rotate((float) Math.toRadians(rotation.y), 0.0F, 1.0F, 0.0F)
                .rotate((float) Math.toRadians(rotation.z), 0.0F, 0.0F, 1.0F);

        return matrix;
    }

    /**
     * Calculates the projection-view matrix for the specified camera.
     * The view part is centered at the camera's position within its chunk.
     *
     * @param camera the camera used for the view and projection
     * @return the combined projection-view matrix
     */
    public static Matrix4f getProjectionViewMatrix(Camera camera) {
        Vector3f position = camera.getPosition().getInChunkPosition();
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-position.x, -position.y, -position.z);

        return matrix;
    }

    /**
     * Calculates a transformation matrix for displaying a structure.
     * The structure is centered and viewed according to the provided zoom and rotation.
     *
     * @param x        the width of the structure in blocks
     * @param y        the height of the structure in blocks
     * @param z        the depth of the structure in blocks
     * @param zoom     the zoom level for the structure view
     * @param rotation the rotation of the structure display
     * @return a Matrix4f for rendering the structure
     */
    public static Matrix4f getStructureDisplayMatrix(int x, int y, int z, float zoom, Vector3f rotation) {
        float centerX = x * 0.5F, centerY = y * 0.5F, centerZ = z * 0.5F;
        float frustumDistance = Math.max(x, Math.max(y, z)) / zoom;
        Vector3f direction = MathUtils.getDirection(rotation).mul(-400.0F);

        Matrix4f matrix = new Matrix4f();
        matrix.ortho(-frustumDistance, frustumDistance, -frustumDistance, frustumDistance, 5000.0F, 50.0F, true);
        matrix.lookAt(centerX + direction.x, centerY + direction.y, centerZ + direction.z, centerX, centerY, centerZ, 0.0F, 1.0F, 0.0F);

        return matrix;
    }

    /**
     * Calculates the matrix used for frustum culling.
     * It uses the camera's projection and rotation, translated by the camera's fractional position.
     *
     * @param camera the camera to use for culling
     * @return the frustum culling matrix
     */
    public static Matrix4f getFrustumCullingMatrix(Camera camera) {
        Position cameraPosition = camera.getPosition();
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-cameraPosition.fractionX, -cameraPosition.fractionY, -cameraPosition.fractionZ);

        return matrix;
    }

    /**
     * Calculates the projection-view matrix for the sun (shadow mapping).
     *
     * @param renderTime the current time used to determine sun position
     * @return the sun's transformation matrix
     */
    public static Matrix4f getSunMatrix(float renderTime) {
        Vector3f sunDirection = getSunDirection(renderTime);
        Matrix4f matrix = new Matrix4f();
        matrix.ortho(-SHADOW_RANGE, SHADOW_RANGE, -SHADOW_RANGE, SHADOW_RANGE, SHADOW_RANGE * 2, 100, true);
        matrix.lookAt(-sunDirection.x * SHADOW_RANGE, -sunDirection.y * SHADOW_RANGE, -sunDirection.z * SHADOW_RANGE,
                0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F);
        return matrix;
    }

    /**
     * Calculates the direction of the sun based on the render time.
     *
     * @param renderTime the current time [0, 1]
     * @return a normalized Vector3f representing sun direction
     */
    public static Vector3f getSunDirection(float renderTime) {
        float alpha = (float) (renderTime * Math.PI);

        return new Vector3f(
                (float) Math.sin(alpha),
                (float) Math.min(Math.cos(alpha), -0.3),
                (float) Math.cos(alpha)
        ).normalize();
    }

    /**
     * Calculates the model matrix for a structure based on its transformation (rotation/mirroring).
     *
     * @param transform the bitmask representing structure transformations
     * @param structure the structure object containing size information
     * @return the model matrix for the transformed structure
     */
    public static Matrix4f getModelMatrix(byte transform, Structure structure) {
        Matrix4f matrix = new Matrix4f();

        if ((transform & Structure.MIRROR_X) != 0) matrix.m00(-matrix.m00()).translate(-structure.sizeX(transform), 0, 0);
        if ((transform & Structure.MIRROR_Z) != 0) matrix.m22(-matrix.m22()).translate(0, 0, -structure.sizeZ(transform));
        if (((transform & Structure.ROTATE_90) != 0)) matrix
                .rotateY((float) Math.PI * 0.5F)
                .translate(-structure.sizeZ(transform), 0, 0);

        return matrix;
    }

    private Transformation() {
    }
}
