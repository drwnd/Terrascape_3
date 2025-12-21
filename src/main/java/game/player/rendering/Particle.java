package game.player.rendering;

public record Particle(int packedOffset, int packedVelocityGravity, int packedLifeTimeRotationMaterial) {

    Particle(int xOffset, int yOffset, int zOffset,
             float velocityX, float velocityY, float velocityZ,
             float rotationSpeedX, float rotationSpeedY,
             float gravity, int lifeTimeTicks, byte material, boolean invertTimeScalar, boolean invertMotion) {
        this(packOffset(xOffset, yOffset, zOffset, invertTimeScalar,invertMotion),
                packVelocityGravity(velocityX, velocityY, velocityZ, gravity),
                packLifeTimeRotationMaterial(lifeTimeTicks, rotationSpeedX, rotationSpeedY, material));
    }

    private static int packOffset(int x, int y, int z, boolean invertTimeScalar, boolean invertMotion) {
        int invertTimeScalarInt = invertTimeScalar ? 1 << 30 : 0;
        int invertMotionInt = invertMotion ? 1 << 31 : 0;
        return invertMotionInt | invertTimeScalarInt | (x + PARTICLE_OFFSET & 0x3FF) << 20 | (y + PARTICLE_OFFSET & 0x3FF) << 10 | z + PARTICLE_OFFSET & 0x3FF;
    }

    private static int packVelocityGravity(float velocityX, float velocityY, float velocityZ, float gravity) {
        velocityX = Math.clamp(velocityX, -31.99F, 31.99F);
        velocityY = Math.clamp(velocityY, -31.99F, 31.99F);
        velocityZ = Math.clamp(velocityZ, -31.99F, 31.99F);
        gravity = Math.clamp(gravity, 0.0F, 127.99F);

        int packedVelocityX = (int) (velocityX * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedVelocityY = (int) (velocityY * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedVelocityZ = (int) (velocityZ * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedGravity = (int) (gravity * GRAVITY_PACKING_FACTOR) & 0xFF;

        return packedVelocityX << 24 | packedVelocityY << 16 | packedVelocityZ << 8 | packedGravity;
    }

    private static int packLifeTimeRotationMaterial(int lifeTime, float rotationSpeedX, float rotationSpeedY, byte material) {
        lifeTime = Math.clamp(lifeTime, 0, 255);
        rotationSpeedX = Math.clamp(rotationSpeedX, 0.0F, 15.99F);
        rotationSpeedY = Math.clamp(rotationSpeedY, 0.0F, 15.99F);

        int packedRotationSpeedX = (int) (rotationSpeedX * ROTATION_PACKING_FACTOR) & 0xFF;
        int packedRotationSpeedY = (int) (rotationSpeedY * ROTATION_PACKING_FACTOR) & 0xFF;

        return lifeTime << 24 | packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | material & 0xFF;
    }

    private static final float VELOCITY_PACKING_FACTOR = 4.0F;  // Inverse in particleVertex.glsl
    private static final float GRAVITY_PACKING_FACTOR = 2.0F;   // Inverse in particleVertex.glsl
    private static final float ROTATION_PACKING_FACTOR = 16.0F; // Inverse in particleVertex.glsl
    private static final int PARTICLE_OFFSET = 512;             // Same in particleVertex.glsl
}
