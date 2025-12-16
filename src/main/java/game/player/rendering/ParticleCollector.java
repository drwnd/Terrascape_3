package game.player.rendering;

import game.server.Game;
import game.server.material.Material;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;
import java.util.Iterator;

import static game.utils.Constants.AIR;
import static game.utils.Constants.OUT_OF_WORLD;

public final class ParticleCollector {

    public static final int SHADER_PARTICLE_INT_SIZE = 3;
    public static final byte PARTICLE_TIME_SHIFT = 20; // Change in particleVertex.glsl

    public void uploadParticleEffects() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) particleEffects.add(loadParticleEffect(particleEffect));
            toBufferParticleEffects.clear();
        }
    }

    public void unloadParticleEffects() {
        int currentTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        for (Iterator<ParticleEffect> iterator = particleEffects.iterator(); iterator.hasNext(); ) {
            ParticleEffect particleEffect = iterator.next();
            if (currentTime - particleEffect.spawnTime() > particleEffect.getLifeTimeNanoSecondsShifted()) {
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

    public void addBreakParticleEffect(int startX, int startY, int startZ, int sideLength, byte ignoreMaterial) {
        ArrayList<Particle> opaqueParticles = new ArrayList<>(sideLength * sideLength);
        ArrayList<Particle> transparentParticles = new ArrayList<>(sideLength * sideLength);

        for (int xOffset = 0; xOffset < sideLength; xOffset++)
            for (int yOffset = 0; yOffset < sideLength; yOffset++)
                for (int zOffset = 0; zOffset < sideLength; zOffset++) {
                    byte particleMaterial = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (particleMaterial == AIR || particleMaterial == OUT_OF_WORLD || particleMaterial == ignoreMaterial) continue;

                    (Material.isGlass(particleMaterial) ? transparentParticles : opaqueParticles)
                            .add(new Particle(xOffset, yOffset, zOffset, particleMaterial, BREAK_PARTICLE_LIFETIME_TICKS,
                                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), BREAK_PARTICLE_GRAVITY,
                                    getRandom(-12f, 12f), getRandom(-2f, 25f), getRandom(-12f, 12f)));
                }

        int spawnTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        if (!opaqueParticles.isEmpty()) {
            int[] particlesData = packParticlesIntoBuffer(opaqueParticles);
            ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, spawnTime, BREAK_PARTICLE_LIFETIME_TICKS, true, startX, startY, startZ);
            addToBufferParticleEffect(effect);
        }
        if (!transparentParticles.isEmpty()) {
            int[] particlesData = packParticlesIntoBuffer(transparentParticles);
            ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, spawnTime, BREAK_PARTICLE_LIFETIME_TICKS, false, startX, startY, startZ);
            addToBufferParticleEffect(effect);
        }
    }

    public void addSplashParticleEffect(int x, int y, int z, byte material) {
        ArrayList<Particle> particles = new ArrayList<>(SPLASH_PARTICLE_COUNT);

        for (int count = 0; count < SPLASH_PARTICLE_COUNT; count++) {
            double angle = 2 * Math.PI * count / SPLASH_PARTICLE_COUNT;

            float velocityX = (float) Math.sin(angle) * 17.0f + getRandom(-3.0f, 3.0f);
            float velocityY = 12.0f + getRandom(-3.0f, 3.0f);
            float velocityZ = (float) Math.cos(angle) * 17.0f + getRandom(-3.0f, 3.0f);

            int xOffset = (int) getRandom(-5.0f, 5.0f);
            int yOffset = (int) getRandom(-5.0f, 5.0f);
            int zOffset = (int) getRandom(-5.0f, 5.0f);

            particles.add(new Particle(xOffset, yOffset, zOffset, material, SPLASH_PARTICLE_LIFETIME_TICKS,
                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), SPLASH_PARTICLE_GRAVITY,
                    velocityX, velocityY, velocityZ));
        }

        int spawnTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        int[] particlesData = packParticlesIntoBuffer(particles);
        addToBufferParticleEffect(new ToBufferParticleEffect(particlesData, spawnTime, SPLASH_PARTICLE_LIFETIME_TICKS, true, x, y, z));
    }


    private void addToBufferParticleEffect(ToBufferParticleEffect particleEffect) {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.add(particleEffect);
        }
    }

    private static float getRandom(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    private static int[] packParticlesIntoBuffer(ArrayList<Particle> particles) {
        int[] particlesData = new int[particles.size() * SHADER_PARTICLE_INT_SIZE];
        int index = 0;

        for (Particle particle : particles) {
            particlesData[index] = particle.packedOffset();
            particlesData[index + 1] = particle.packedVelocityGravity();
            particlesData[index + 2] = particle.packedLifeTimeRotationTexture();

            index += SHADER_PARTICLE_INT_SIZE;
        }

        return particlesData;
    }

    private static ParticleEffect loadParticleEffect(ToBufferParticleEffect particleEffect) {
        int particlesBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(particlesBuffer, particleEffect.particlesData(), GL46.GL_STATIC_DRAW);

        return new ParticleEffect(particlesBuffer, particleEffect.spawnTime(),
                particleEffect.lifeTimeTicks(), particleEffect.particlesData().length / SHADER_PARTICLE_INT_SIZE,
                particleEffect.isOpaque(), particleEffect.x(), particleEffect.y(), particleEffect.z());
    }


    private final ArrayList<ParticleEffect> particleEffects = new ArrayList<>();
    private final ArrayList<ToBufferParticleEffect> toBufferParticleEffects = new ArrayList<>();

    private static final int BREAK_PARTICLE_LIFETIME_TICKS = 40;
    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final int SPLASH_PARTICLE_LIFETIME_TICKS = 20;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;

    public record ToBufferParticleEffect(int[] particlesData, int spawnTime, int lifeTimeTicks, boolean isOpaque, int x, int y, int z) {
    }
}
