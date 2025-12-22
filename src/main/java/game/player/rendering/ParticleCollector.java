package game.player.rendering;

import core.settings.ToggleSetting;
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
        if (!ToggleSetting.SHOW_BREAK_PARTICLES.value()) return;
        ArrayList<Particle> opaqueParticles = new ArrayList<>(sideLength * sideLength);
        ArrayList<Particle> transparentParticles = new ArrayList<>(sideLength * sideLength);

        for (int xOffset = 0; xOffset < sideLength; xOffset++)
            for (int yOffset = 0; yOffset < sideLength; yOffset++)
                for (int zOffset = 0; zOffset < sideLength; zOffset++) {
                    byte material = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (material == AIR || material == OUT_OF_WORLD || material == ignoreMaterial) continue;

                    (Material.isGlass(material) ? transparentParticles : opaqueParticles)
                            .add(new Particle(xOffset, yOffset, zOffset,
                                    getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                                    getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, BREAK_PARTICLE_LIFETIME_TICKS, material,
                                    false, false));
                }

        addParticles(startX, startY, startZ, opaqueParticles, BREAK_PARTICLE_LIFETIME_TICKS, true);
        addParticles(startX, startY, startZ, transparentParticles, BREAK_PARTICLE_LIFETIME_TICKS, false);
    }
    
    public void addPlaceParticleEffect(int startX, int startY, int startZ, int sideLength, byte material) {
        if (!ToggleSetting.SHOW_CUBE_PLACE_PARTICLES.value() || material == AIR) return;
        ArrayList<Particle> particles = new ArrayList<>(sideLength * sideLength * sideLength);

        for (int xOffset = 0; xOffset < sideLength; xOffset++)
            for (int yOffset = 0; yOffset < sideLength; yOffset++)
                for (int zOffset = 0; zOffset < sideLength; zOffset++) {
                    particles.add(new Particle(xOffset, yOffset, zOffset,
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, PLACE_PARTICLE_LIFETIME_TICKS, material,
                            true, true));
                }
        
        addParticles(startX, startY, startZ, particles, PLACE_PARTICLE_LIFETIME_TICKS, !Material.isGlass(material));
    }

    public void addPlaceParticleEffect(int startX, int startY, int startZ, Structure structure) {
        if (!ToggleSetting.SHOW_STRUCTURE_PLACE_PARTICLES.value()) return;
        ArrayList<Particle> opaqueParticles = new ArrayList<>(structure.sizeX() * structure.sizeZ());
        ArrayList<Particle> transparentParticles = new ArrayList<>(structure.sizeX() * structure.sizeZ());
        MaterialsData materials = structure.materials();

        for (int xOffset = 0; xOffset < structure.sizeX(); xOffset++)
            for (int yOffset = 0; yOffset < structure.sizeY(); yOffset++)
                for (int zOffset = 0; zOffset < structure.sizeZ(); zOffset++) {
                    byte material = materials.getMaterial(xOffset, yOffset, zOffset);
                    if (material == AIR || material == OUT_OF_WORLD) continue;

                    (Material.isGlass(material) ? transparentParticles : opaqueParticles)
                            .add(new Particle(xOffset, yOffset, zOffset,
                                    getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                                    getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, PLACE_PARTICLE_LIFETIME_TICKS, material,
                                    true, true));
                }

        addParticles(startX, startY, startZ, opaqueParticles, BREAK_PARTICLE_LIFETIME_TICKS, true);
        addParticles(startX, startY, startZ, transparentParticles, BREAK_PARTICLE_LIFETIME_TICKS, false);
    }

    private void addParticles(int startX, int startY, int startZ, ArrayList<Particle> particles, int lifeTime, boolean isOpaque) {
        int spawnTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        if (particles.isEmpty()) return;
        int[] particlesData = packParticlesIntoBuffer(particles);
        ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, spawnTime, lifeTime, isOpaque, startX, startY, startZ);
        addToBufferParticleEffect(effect);
    }

    public void addSplashParticleEffect(int x, int y, int z, byte material) {
        if (!ToggleSetting.SHOW_SPLASH_PARTICLES.value()) return;
        ArrayList<Particle> particles = new ArrayList<>(SPLASH_PARTICLE_COUNT);

        for (int count = 0; count < SPLASH_PARTICLE_COUNT; count++) {
            double angle = 2 * Math.PI * count / SPLASH_PARTICLE_COUNT;

            float velocityX = (float) Math.sin(angle) * 17.0F + getRandom(-3.0F, 3.0F);
            float velocityY = 12.0F + getRandom(-3.0F, 3.0F);
            float velocityZ = (float) Math.cos(angle) * 17.0F + getRandom(-3.0F, 3.0F);

            int xOffset = (int) getRandom(-5.0F, 5.0F);
            int yOffset = (int) getRandom(-5.0F, 5.0F);
            int zOffset = (int) getRandom(-5.0F, 5.0F);

            particles.add(new Particle(xOffset, yOffset, zOffset, velocityX, velocityY, velocityZ, getRandom(0.0F, 5F), getRandom(0.0F, 5F), SPLASH_PARTICLE_GRAVITY, SPLASH_PARTICLE_LIFETIME_TICKS, material,
                    false, false));
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
            particlesData[index + 2] = particle.packedLifeTimeRotationMaterial();

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

    private static final int PLACE_PARTICLE_LIFETIME_TICKS = 10;
    private static final int BREAK_PARTICLE_LIFETIME_TICKS = 40;
    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final int SPLASH_PARTICLE_LIFETIME_TICKS = 20;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;

    public record ToBufferParticleEffect(int[] particlesData, int spawnTime, int lifeTimeTicks, boolean isOpaque, int x, int y, int z) {
    }
}
