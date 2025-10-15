package game.utils;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import game.player.rendering.Camera;

public final class Transformation {

    public static Matrix4f createProjectionRotationMatrix(Camera camera) {
        Vector3f rotation = camera.getRotation();

        Matrix4f matrix = new Matrix4f(camera.getProjectionMatrix());
        matrix.rotate((float) Math.toRadians(rotation.x), X_AXIS)
                .rotate((float) Math.toRadians(rotation.y), Y_AXIS)
                .rotate((float) Math.toRadians(rotation.z), Z_AXIS);

        return matrix;
    }

    public static Matrix4f getProjectionViewMatrix(Camera camera) {
        Vector3f position = camera.getPosition().getInChunkPosition();
        return getProjectionViewMatrix(camera, position);
    }

    private static Matrix4f getProjectionViewMatrix(Camera camera, Vector3f translation) {
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-translation.x, -translation.y, -translation.z);

        return matrix;
    }

    public static Matrix4f getStructureDisplayMatrix(int x, int y, int z, float zoom) {
        float centerX = x * 0.5f, centerY = y * 0.5f, centerZ = z * 0.5f;
        Matrix4f matrix = new Matrix4f();
        int maxSize = Math.max(x, Math.max(y, z));

        matrix.ortho(-maxSize / zoom, maxSize / zoom, -maxSize / zoom, maxSize / zoom, 50, 50000);
        matrix.lookAt(centerX + 2000, centerY + 2000, centerZ + 2000, centerX, centerY, centerZ, Y_AXIS.x, Y_AXIS.y, Y_AXIS.z);

        return matrix;
    }

    public static Matrix4f getFrustumCullingMatrix(Camera camera) {
        Position cameraPosition = camera.getPosition();
        Matrix4f matrix = createProjectionRotationMatrix(camera);
        matrix.translate(-cameraPosition.intX, -cameraPosition.intY, -cameraPosition.intZ);
        matrix.translate(-cameraPosition.fractionX, -cameraPosition.fractionY, -cameraPosition.fractionZ);

        return matrix;
    }

    private Transformation() {
    }

    private static final Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    private static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);
}
