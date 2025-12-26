package game.player.rendering;

import core.settings.ToggleSetting;
import core.utils.IntArrayList;
import game.server.Game;
import game.server.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Material;

import org.lwjgl.opengl.GL46;

import java.util.ArrayList;
import java.util.Iterator;

import static game.utils.Constants.*;

public final class ParticleCollector {

    public static final int SHADER_PARTICLE_INT_SIZE = 3;

    public void uploadParticleEffects() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) particleEffects.add(loadParticleEffect(particleEffect));
            toBufferParticleEffects.clear();
        }
    }

    public void unloadParticleEffects() {
        long currentTick = Game.getServer().getCurrentGameTick();
        for (Iterator<ParticleEffect> iterator = particleEffects.iterator(); iterator.hasNext(); ) {
            ParticleEffect particleEffect = iterator.next();
            if (currentTick - particleEffect.spawnTick() >= particleEffect.lifeTimeTicks()) {
                iterator.remove();
                GL46.glDeleteBuffers(particleEffect.buffer());
            }
        }
    }

    public void cleanUp() {
        synchronized (particleEffects) {
            for (ParticleEffect particleEffect : particleEffects) GL46.glDeleteBuffers(particleEffect.buffer());
            particleEffects.clear();
        }
    }

    public ArrayList<ParticleEffect> getParticleEffects() {
        return particleEffects;
    }

    public void addBreakParticleEffect(int startX, int startY, int startZ, int lengthX, int lengthY, int lengthZ, byte ignoreMaterial) {
        if (!ToggleSetting.SHOW_BREAK_PARTICLES.value()) return;
        IntArrayList opaqueParticles = new IntArrayList(lengthX * lengthY * lengthZ);
        IntArrayList transparentParticles = new IntArrayList(lengthX * lengthY * lengthZ);

        for (int xOffset = 0; xOffset < lengthX; xOffset++)
            for (int yOffset = 0; yOffset < lengthY; yOffset++)
                for (int zOffset = 0; zOffset < lengthZ; zOffset++) {
                    byte material = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (material == AIR || material == OUT_OF_WORLD || material == ignoreMaterial) continue;

                    add(Material.isGlass(material) ? transparentParticles : opaqueParticles,
                            xOffset, yOffset, zOffset,
                            getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, material,
                            false, false);
                }

        addParticles(startX, startY, startZ, opaqueParticles, BREAK_PARTICLE_LIFETIME_TICKS, true);
        addParticles(startX, startY, startZ, transparentParticles, BREAK_PARTICLE_LIFETIME_TICKS, false);
    }

    public void addPlaceParticleEffect(int startX, int startY, int startZ, int lengthX, int lengthY, int lengthZ, byte material) {
        if (!ToggleSetting.SHOW_CUBE_PLACE_PARTICLES.value() || material == AIR) return;
        IntArrayList particles = new IntArrayList(lengthX * lengthY * lengthZ * SHADER_PARTICLE_INT_SIZE);

        for (int xOffset = 0; xOffset < lengthX; xOffset++)
            for (int yOffset = 0; yOffset < lengthY; yOffset++)
                for (int zOffset = 0; zOffset < lengthZ; zOffset++) {
                    add(particles,
                            xOffset, yOffset, zOffset,
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);
                }

        addParticles(startX, startY, startZ, particles, PLACE_PARTICLE_LIFETIME_TICKS, !Material.isGlass(material));
    }

    public void addPlaceParticleEffect(int startX, int startY, int startZ, Structure structure) {
        if (!ToggleSetting.SHOW_STRUCTURE_PLACE_PARTICLES.value()) return;
        IntArrayList opaqueParticles = new IntArrayList(structure.sizeX() * structure.sizeZ());
        IntArrayList transparentParticles = new IntArrayList(structure.sizeX() * structure.sizeZ());
        MaterialsData materials = structure.materials();

        for (int xOffset = 0; xOffset < structure.sizeX(); xOffset++)
            for (int yOffset = 0; yOffset < structure.sizeY(); yOffset++)
                for (int zOffset = 0; zOffset < structure.sizeZ(); zOffset++) {
                    byte material = materials.getMaterial(xOffset, yOffset, zOffset);
                    if (material == AIR || material == OUT_OF_WORLD) continue;

                    add(Material.isGlass(material) ? transparentParticles : opaqueParticles,
                            xOffset, yOffset, zOffset,
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);
                }

        addParticles(startX, startY, startZ, opaqueParticles, PLACE_PARTICLE_LIFETIME_TICKS, true);
        addParticles(startX, startY, startZ, transparentParticles, PLACE_PARTICLE_LIFETIME_TICKS, false);
    }

    private void addParticles(int startX, int startY, int startZ, IntArrayList particles, int lifeTime, boolean isOpaque) {
        long currentTick = Game.getServer().getCurrentGameTick();
        if (particles.isEmpty()) return;
        int[] particlesData = packParticlesIntoBuffer(particles);
        ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, currentTick, lifeTime, isOpaque, startX, startY, startZ);
        addToBufferParticleEffect(effect);
    }

    public void addSplashParticleEffect(int x, int y, int z, byte material) {
        if (!ToggleSetting.SHOW_SPLASH_PARTICLES.value()) return;
        IntArrayList particles = new IntArrayList(SPLASH_PARTICLE_COUNT * SHADER_PARTICLE_INT_SIZE);

        for (int count = 0; count < SPLASH_PARTICLE_COUNT; count++) {
            double angle = 2 * Math.PI * count / SPLASH_PARTICLE_COUNT;

            float velocityX = (float) Math.sin(angle) * 17.0F + getRandom(-3.0F, 3.0F);
            float velocityY = 12.0F + getRandom(-3.0F, 3.0F);
            float velocityZ = (float) Math.cos(angle) * 17.0F + getRandom(-3.0F, 3.0F);

            int xOffset = (int) getRandom(-5.0F, 5.0F);
            int yOffset = (int) getRandom(-5.0F, 5.0F);
            int zOffset = (int) getRandom(-5.0F, 5.0F);

            add(particles,
                    xOffset, yOffset, zOffset,
                    velocityX, velocityY, velocityZ,
                    getRandom(0.0F, 5F), getRandom(0.0F, 5F),
                    SPLASH_PARTICLE_GRAVITY, material,
                    false, false);
        }

        long spawnTick = Game.getServer().getCurrentGameTick();
        int[] particlesData = packParticlesIntoBuffer(particles);
        addToBufferParticleEffect(new ToBufferParticleEffect(particlesData, spawnTick, SPLASH_PARTICLE_LIFETIME_TICKS, true, x, y, z));
    }


    private void addToBufferParticleEffect(ToBufferParticleEffect particleEffect) {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.add(particleEffect);
        }
    }

    private static float getRandom(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    private static int[] packParticlesIntoBuffer(IntArrayList particles) {
        int[] particlesData = new int[particles.size()];
        particles.copyInto(particlesData, 0);

        return particlesData;
    }

    private static ParticleEffect loadParticleEffect(ToBufferParticleEffect particleEffect) {
        int particlesBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(particlesBuffer, particleEffect.particlesData(), GL46.GL_STATIC_DRAW);

        return new ParticleEffect(particlesBuffer, particleEffect.spawnTick(),
                particleEffect.lifeTimeTicks(), particleEffect.particlesData().length / SHADER_PARTICLE_INT_SIZE,
                particleEffect.isOpaque(), particleEffect.x(), particleEffect.y(), particleEffect.z());
    }

    private static void add(IntArrayList particles,
                            int xOffset, int yOffset, int zOffset,
                            float velocityX, float velocityY, float velocityZ,
                            float rotationSpeedX, float rotationSpeedY,
                            float gravity, byte material, boolean disableTimeScalar, boolean invertMotion) {

        particles.add(packOffset(xOffset, yOffset, zOffset, disableTimeScalar, invertMotion));
        particles.add(packVelocityGravity(velocityX, velocityY, velocityZ, gravity));
        particles.add(packRotationMaterial(rotationSpeedX, rotationSpeedY, material));
    }

    private static int packOffset(int x, int y, int z, boolean diableTimeScalar, boolean invertMotion) {
        int invertTimeScalarInt = diableTimeScalar ? 1 << 30 : 0;
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

    private static int packRotationMaterial(float rotationSpeedX, float rotationSpeedY, byte material) {
        rotationSpeedX = Math.clamp(rotationSpeedX, 0.0F, 15.99F);
        rotationSpeedY = Math.clamp(rotationSpeedY, 0.0F, 15.99F);

        int packedRotationSpeedX = (int) (rotationSpeedX * ROTATION_PACKING_FACTOR) & 0xFF;
        int packedRotationSpeedY = (int) (rotationSpeedY * ROTATION_PACKING_FACTOR) & 0xFF;

        return packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | material & 0xFF;
    }


    private final ArrayList<ParticleEffect> particleEffects = new ArrayList<>();
    private final ArrayList<ToBufferParticleEffect> toBufferParticleEffects = new ArrayList<>();

    private static final float VELOCITY_PACKING_FACTOR = 4.0F;  // Inverse in Particle.vert
    private static final float GRAVITY_PACKING_FACTOR = 2.0F;   // Inverse in Particle.vert
    private static final float ROTATION_PACKING_FACTOR = 16.0F; // Inverse in Particle.vert
    private static final int PARTICLE_OFFSET = 512;             // Same in Particle.vert

    private static final int PLACE_PARTICLE_LIFETIME_TICKS = 10;
    private static final int BREAK_PARTICLE_LIFETIME_TICKS = 40;
    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final int SPLASH_PARTICLE_LIFETIME_TICKS = 20;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;

    public record ToBufferParticleEffect(int[] particlesData, long spawnTick, int lifeTimeTicks, boolean isOpaque, int x, int y, int z) {
    }
}
