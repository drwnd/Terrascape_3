package game.utils;

import core.utils.MathUtils;

import game.player.rendering.Camera;
import game.server.generation.Structure;
import game.settings.OptionSettings;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static game.utils.Constants.*;

public final class Transformation {

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

    public static Matrix4f getProjectionViewMatrix(Camera camera) {
        Vector3f position = camera.getPosition().getInChunkPosition();
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-position.x, -position.y, -position.z);

        return matrix;
    }

    public static Matrix4f getStructureDisplayMatrix(int x, int y, int z, float zoom, Vector3f rotation) {
        float centerX = x * 0.5F, centerY = y * 0.5F, centerZ = z * 0.5F;
        float frustumDistance = Math.max(x, Math.max(y, z)) / zoom;
        Vector3f direction = MathUtils.getDirection(rotation).mul(-400.0F);

        Matrix4f matrix = new Matrix4f();
        matrix.ortho(-frustumDistance, frustumDistance, -frustumDistance, frustumDistance, 5000.0F, 50.0F, true);
        matrix.lookAt(centerX + direction.x, centerY + direction.y, centerZ + direction.z, centerX, centerY, centerZ, 0.0F, 1.0F, 0.0F);

        return matrix;
    }

    public static Matrix4f getFrustumCullingMatrix(Camera camera) {
        Position cameraPosition = camera.getPosition();
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-cameraPosition.fractionX, -cameraPosition.fractionY, -cameraPosition.fractionZ);

        return matrix;
    }

    public static void updateSunMatrix(Matrix4f sunMatrix, Position snapshotPosition, Position cameraPosition, float renderTime, int cascade) {
        Vector3f sunDirection = getSunDirection(renderTime);
        int shadowRange = SHADOW_RANGE << cascade;
        Vector3f movement = snapshotPosition == null ? new Vector3f() : new Vector3f(
                (cameraPosition.longX & ~CHUNK_SIZE_MASK) - (snapshotPosition.longX & ~CHUNK_SIZE_MASK),
                (cameraPosition.longY & ~CHUNK_SIZE_MASK) - (snapshotPosition.longY & ~CHUNK_SIZE_MASK),
                (cameraPosition.longZ & ~CHUNK_SIZE_MASK) - (snapshotPosition.longZ & ~CHUNK_SIZE_MASK)
        );
        sunMatrix.identity()
                .ortho(-shadowRange, shadowRange, -shadowRange, shadowRange, shadowRange * 2, 100, true)
                .lookAt(-sunDirection.x * shadowRange, -sunDirection.y * shadowRange, -sunDirection.z * shadowRange,
                0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F)
                .translate(movement);
    }

    public static Vector3f getSunDirection(float renderTime) {
        float alpha = (float) (renderTime * Math.PI);

        return new Vector3f(
                (float) Math.sin(alpha),
                (float) Math.min(Math.cos(alpha), -0.3),
                (float) Math.cos(alpha)
        ).normalize();
    }

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
