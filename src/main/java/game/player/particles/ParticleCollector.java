package game.player.particles;

import core.utils.IntArrayList;

import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.player.rendering.MeshGenerator;
import game.server.Game;
import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Material;
import game.settings.IntSettings;
import game.settings.OptionSettings;
import game.settings.ToggleSettings;

import org.joml.Random;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Iterator;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class ParticleCollector {

    public static final int SHADER_PARTICLE_INT_SIZE = 3;

    /**
     * Performs upload particle effects.
     */
    public void uploadParticleEffects() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) particleEffects.add(loadParticleEffect(particleEffect));
        }
    }

    /**
     * Performs play particle effect sounds.
     */
    public void playParticleEffectSounds() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) ParticleType.playSound(particleEffect);
        }
    }

    /**
     * Performs clear to buffer particle effects.
     */
    public void clearToBufferParticleEffects() {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.clear();
        }
    }

    /**
     * Performs unload particle effects.
     */
    public void unloadParticleEffects() {
        long currentTick = Game.getServer().getCurrentGameTick();
        for (Iterator<ParticleEffect> iterator = particleEffects.iterator(); iterator.hasNext(); ) {
            ParticleEffect particleEffect = iterator.next();
            if (currentTick - particleEffect.spawnTick() >= particleEffect.lifeTimeTicks()) {
                iterator.remove();
                glDeleteBuffers(particleEffect.buffer());
            }
        }
    }

    /**
     * Performs clean up.
     */
    public void cleanUp() {
        synchronized (particleEffects) {
            for (ParticleEffect particleEffect : particleEffects) glDeleteBuffers(particleEffect.buffer());
            particleEffects.clear();
        }
    }

    public ArrayList<ParticleEffect> getParticleEffects() {
        return particleEffects;
    }

    /**
     * Adds break place particle effect.
     *
     * @param startX    X coordinate in local block coordinates
     * @param startY    Y coordinate in local block coordinates
     * @param startZ    Z coordinate in local block coordinates
     * @param countX    extent along the X axis in local block coordinates
     * @param countY    extent along the Y axis in local block coordinates
     * @param countZ    extent along the Z axis in local block coordinates
     * @param placeable parameter
     */
    public void addBreakPlaceParticleEffect(long startX, long startY, long startZ, int countX, int countY, int countZ, ShapePlaceable placeable) {
        boolean addBreakEffect = ToggleSettings.SHOW_BREAK_PARTICLES.value();
        boolean addPlaceEffect = ToggleSettings.SHOW_SHAPE_PLACE_PARTICLES.value() && placeable.getMaterial() != AIR;
        if (!addBreakEffect && !addPlaceEffect) return;

        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();

        IntArrayList opaqueParticles = new IntArrayList(addBreakEffect ? placeable.getPreferredSize() * countX * countY * countZ : 0);
        IntArrayList transparentParticles = new IntArrayList(addBreakEffect ? placeable.getPreferredSize() * countX * countY * countZ : 0);
        IntArrayList placeParticles = new IntArrayList(addPlaceEffect ? placeable.getPreferredSize() * countX * countY * countZ : 0);

        for (long x = startX; x < startX + (long) countX * lengthX; x += lengthX)
            for (long y = startY; y < startY + (long) countY * lengthY; y += lengthY)
                for (long z = startZ; z < startZ + (long) countZ * lengthZ; z += lengthZ) {
                    if (addBreakEffect)
                        addBreakEffectLoop((int) (x - startX), (int) (y - startY), (int) (z - startZ), x, y, z, transparentParticles, opaqueParticles, placeable);
                    if (addPlaceEffect)
                        addPlaceEffectLoop((int) (x - startX), (int) (y - startY), (int) (z - startZ), x, y, z, placeParticles, placeable);
                }

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_BREAK);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_BREAK);
        addParticles(startX, startY, startZ, placeParticles, Material.isGlass(placeable.getMaterial()) ? ParticleType.TRANSPARENT_PLACE : ParticleType.OPAQUE_PLACE);
    }

    /**
     * Adds place particle effect.
     *
     * @param startX    X coordinate in local block coordinates
     * @param startY    Y coordinate in local block coordinates
     * @param startZ    Z coordinate in local block coordinates
     * @param structure parameter
     * @param lengths   3D vector in local block coordinates
     * @param transform parameter
     */
    public void addPlaceParticleEffect(long startX, long startY, long startZ, Structure structure, Vector3i lengths, byte transform) {
        if (!ToggleSettings.SHOW_STRUCTURE_PLACE_PARTICLES.value()) return;
        IntArrayList opaqueParticles = new IntArrayList(structure.sizeX() * structure.sizeZ());
        IntArrayList transparentParticles = new IntArrayList(structure.sizeX() * structure.sizeZ());

        structure.materials().addPlaceParticles(this, opaqueParticles, transparentParticles, lengths, transform);

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_PLACE);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_PLACE);
    }

    /**
     * Adds splash particle effect.
     *
     * @param x        X coordinate in local block coordinates
     * @param y        Y coordinate in local block coordinates
     * @param z        Z coordinate in local block coordinates
     * @param material parameter
     */
    public void addSplashParticleEffect(int x, int y, int z, byte material) {
        if (!ToggleSettings.SHOW_SPLASH_PARTICLES.value()) return;
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

        addParticles(x, y, z, particles, ParticleType.SPLASH);
    }


    /**
     * Adds particles.
     *
     * @param startX    X coordinate in local block coordinates
     * @param startY    Y coordinate in local block coordinates
     * @param startZ    Z coordinate in local block coordinates
     * @param particles parameter
     * @param type      parameter
     */
    private void addParticles(long startX, long startY, long startZ, IntArrayList particles, ParticleType type) {
        long currentTick = Game.getServer().getCurrentGameTick();
        if (particles.isEmpty()) return;
        int[] particlesData = packParticlesIntoBuffer(particles);
        ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, currentTick, type, startX, startY, startZ);
        addToBufferParticleEffect(effect);
    }

    /**
     * Adds to buffer particle effect.
     *
     * @param particleEffect parameter
     */
    private void addToBufferParticleEffect(ToBufferParticleEffect particleEffect) {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.add(particleEffect);
        }
    }

    /**
     * Adds place effect loop.
     *
     * @param startX         X coordinate in local block coordinates
     * @param startY         Y coordinate in local block coordinates
     * @param startZ         Z coordinate in local block coordinates
     * @param x              X coordinate in local block coordinates
     * @param y              Y coordinate in local block coordinates
     * @param z              Z coordinate in local block coordinates
     * @param placeParticles parameter
     * @param placeable      parameter
     */
    private void addPlaceEffectLoop(int startX, int startY, int startZ,
                                    long x, long y, long z,
                                    IntArrayList placeParticles, ShapePlaceable placeable) {
        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();
        long[] bitMap = placeable.getBitMap();
        byte material = placeable.getMaterial();

        int stepLength = IntSettings.PLACE_PARTICLE_STEP_LENGTH.value();
        for (int xOffset = 0; xOffset < lengthX; xOffset += stepLength)
            for (int yOffset = 0; yOffset < lengthY; yOffset += stepLength)
                for (int zOffset = 0; zOffset < lengthZ; zOffset += stepLength) {

                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(x + xOffset, y + yOffset, z + zOffset, 0);
                    if (previousMaterial == material || paint && previousMaterial == AIR || replaceAir && previousMaterial != AIR) continue;

                    addPlaceParticle(placeParticles, bitMap,
                            lengthX, lengthY, lengthZ,
                            xOffset + startX, yOffset + startY, zOffset + startZ,
                            material);
                }
    }

    /**
     * Adds break effect loop.
     *
     * @param startX               X coordinate in local block coordinates
     * @param startY               Y coordinate in local block coordinates
     * @param startZ               Z coordinate in local block coordinates
     * @param x                    X coordinate in local block coordinates
     * @param y                    Y coordinate in local block coordinates
     * @param z                    Z coordinate in local block coordinates
     * @param transparentParticles parameter
     * @param opaqueParticles      parameter
     * @param placeable            parameter
     */
    private void addBreakEffectLoop(int startX, int startY, int startZ,
                                    long x, long y, long z,
                                    IntArrayList transparentParticles, IntArrayList opaqueParticles, ShapePlaceable placeable) {
        if (OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR) return;
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        byte heldMaterial = breakHeldOnly ? ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial() : AIR;
        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();
        long[] bitMap = placeable.getBitMap();
        byte material = placeable.getMaterial();

        int stepLength = IntSettings.BREAK_PARTICLE_STEP_LENGTH.value();
        for (int xOffset = 0; xOffset < lengthX; xOffset += stepLength)
            for (int yOffset = 0; yOffset < lengthY; yOffset += stepLength)
                for (int zOffset = 0; zOffset < lengthZ; zOffset += stepLength) {

                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(x + xOffset, y + yOffset, z + zOffset, 0);
                    if (previousMaterial == AIR || previousMaterial == OUT_OF_WORLD
                            || previousMaterial == material
                            || breakHeldOnly && previousMaterial != heldMaterial) continue;

                    addBreakParticle(Material.isGlass(previousMaterial) ? transparentParticles : opaqueParticles,
                            xOffset + startX, yOffset + startY, zOffset + startZ,
                            previousMaterial);
                }
    }

    private float getRandom(float min, float max) {
        return random.nextFloat() * (max - min) + min;
    }

    /**
     * Performs pack particles into buffer.
     *
     * @param particles parameter
     * @return array result
     */
    private static int[] packParticlesIntoBuffer(IntArrayList particles) {
        int[] particlesData = new int[particles.size()];
        particles.copyInto(particlesData, 0);

        return particlesData;
    }

    /**
     * Performs load particle effect.
     *
     * @param particleEffect parameter
     * @return result
     */
    private static ParticleEffect loadParticleEffect(ToBufferParticleEffect particleEffect) {
        int particlesBuffer = glCreateBuffers();
        glNamedBufferData(particlesBuffer, particleEffect.particlesData(), GL_STATIC_DRAW);

        return new ParticleEffect(particlesBuffer, particleEffect.spawnTick(),
                particleEffect.type().getLifeTimeTicks(), particleEffect.particlesData().length / SHADER_PARTICLE_INT_SIZE,
                particleEffect.type().isOpaque(), particleEffect.x(), particleEffect.y(), particleEffect.z());
    }

    /**
     * Adds place particle.
     *
     * @param particles parameter
     * @param bitMap    parameter
     * @param lengthX   extent along the X axis in local block coordinates
     * @param lengthY   extent along the Y axis in local block coordinates
     * @param lengthZ   extent along the Z axis in local block coordinates
     * @param xOffset   parameter
     * @param yOffset   parameter
     * @param zOffset   parameter
     * @param material  parameter
     * @param transform parameter
     */
    public void addPlaceParticle(IntArrayList particles, long[] bitMap,
                                 int lengthX, int lengthY, int lengthZ,
                                 int xOffset, int yOffset, int zOffset,
                                 byte material, byte transform) {
        float velocityX = getRandom(-8F, 8F), velocityY = getRandom(-8F, 8F), velocityZ = getRandom(-8F, 8F);
        float rotationSpeedX = getRandom(0.0F, 5F), rotationSpeedY = getRandom(0.0F, 5F);

        if (checkParticleVisibility(bitMap, lengthX, lengthY, lengthZ, xOffset, yOffset, zOffset, velocityX, velocityY, velocityZ)) return;

        if (transform == Structure.ROTATE_90 || transform == Structure.ALL_TRANSFORMS) transform ^= Structure.MIRROR_X | Structure.MIRROR_Z;
        if ((transform & Structure.MIRROR_X) != 0) {
            xOffset = lengthX - xOffset - 1;
            velocityX = -velocityX;
        }
        if ((transform & Structure.MIRROR_Z) != 0) {
            zOffset = lengthZ - zOffset - 1;
            velocityZ = -velocityZ;
        }
        if ((transform & Structure.ROTATE_90) != 0) {
            int offsetCopy = zOffset;
            float velocityCopy = velocityZ;

            zOffset = xOffset;
            velocityZ = velocityX;

            xOffset = lengthZ - offsetCopy - 1;
            velocityX = -velocityCopy;
        }

        particles.add(packOffset(xOffset, yOffset, zOffset, true, true));
        particles.add(packVelocityGravity(velocityX, velocityY, velocityZ, 0.0F));
        particles.add(packRotationMaterial(rotationSpeedX, rotationSpeedY, material));
    }

    /**
     * Adds place particle.
     *
     * @param particles parameter
     * @param bitMap    parameter
     * @param lengthX   extent along the X axis in local block coordinates
     * @param lengthY   extent along the Y axis in local block coordinates
     * @param lengthZ   extent along the Z axis in local block coordinates
     * @param xOffset   parameter
     * @param yOffset   parameter
     * @param zOffset   parameter
     * @param material  parameter
     */
    public void addPlaceParticle(IntArrayList particles, long[] bitMap,
                                 int lengthX, int lengthY, int lengthZ,
                                 int xOffset, int yOffset, int zOffset,
                                 byte material) {
        float velocityX = getRandom(-8F, 8F), velocityY = getRandom(-8F, 8F), velocityZ = getRandom(-8F, 8F);
        float rotationSpeedX = getRandom(0.0F, 5F), rotationSpeedY = getRandom(0.0F, 5F);

        if (checkParticleVisibility(bitMap, lengthX, lengthY, lengthZ, xOffset, yOffset, zOffset, velocityX, velocityY, velocityZ)) return;

        particles.add(packOffset(xOffset, yOffset, zOffset, true, true));
        particles.add(packVelocityGravity(velocityX, velocityY, velocityZ, 0.0F));
        particles.add(packRotationMaterial(rotationSpeedX, rotationSpeedY, material));
    }

    /**
     * Adds break particle.
     *
     * @param particles parameter
     * @param xOffset   parameter
     * @param yOffset   parameter
     * @param zOffset   parameter
     * @param material  parameter
     */
    public void addBreakParticle(IntArrayList particles, int xOffset, int yOffset, int zOffset, byte material) {
        float velocityX = getRandom(-12F, 12F), velocityY = getRandom(-2F, 25F), velocityZ = getRandom(-12F, 12F);
        float rotationSpeedX = getRandom(0.0F, 5F), rotationSpeedY = getRandom(0.0F, 5F);

        particles.add(packOffset(xOffset, yOffset, zOffset, false, false));
        particles.add(packVelocityGravity(velocityX, velocityY, velocityZ, BREAK_PARTICLE_GRAVITY));
        particles.add(packRotationMaterial(rotationSpeedX, rotationSpeedY, material));
    }

    /**
     * Performs add.
     *
     * @param particles         parameter
     * @param xOffset           parameter
     * @param yOffset           parameter
     * @param zOffset           parameter
     * @param velocityX         X coordinate in local block coordinates
     * @param velocityY         Y coordinate in local block coordinates
     * @param velocityZ         Z coordinate in local block coordinates
     * @param rotationSpeedX    X coordinate in local block coordinates
     * @param rotationSpeedY    Y coordinate in local block coordinates
     * @param gravity           Y coordinate in local block coordinates
     * @param material          parameter
     * @param disableTimeScalar parameter
     * @param invertMotion      parameter
     */
    public static void add(IntArrayList particles,
                           int xOffset, int yOffset, int zOffset,
                           float velocityX, float velocityY, float velocityZ,
                           float rotationSpeedX, float rotationSpeedY,
                           float gravity, byte material, boolean disableTimeScalar, boolean invertMotion) {

        particles.add(packOffset(xOffset, yOffset, zOffset, disableTimeScalar, invertMotion));
        particles.add(packVelocityGravity(velocityX, velocityY, velocityZ, gravity));
        particles.add(packRotationMaterial(rotationSpeedX, rotationSpeedY, material));
    }

    /**
     * Performs check particle visibility.
     *
     * @param bitMap    parameter
     * @param lengthX   extent along the X axis in local block coordinates
     * @param lengthY   extent along the Y axis in local block coordinates
     * @param lengthZ   extent along the Z axis in local block coordinates
     * @param xOffset   parameter
     * @param yOffset   parameter
     * @param zOffset   parameter
     * @param velocityX X coordinate in local block coordinates
     * @param velocityY Y coordinate in local block coordinates
     * @param velocityZ Z coordinate in local block coordinates
     * @return true if the condition holds
     */
    private static boolean checkParticleVisibility(long[] bitMap, int lengthX, int lengthY, int lengthZ,
                                                   int xOffset, int yOffset, int zOffset,
                                                   float velocityX, float velocityY, float velocityZ) {
        int sampleX = xOffset + (int) (velocityX * ParticleType.OPAQUE_PLACE.getLifeTimeTicks() * 0.05F);
        int sampleY = yOffset + (int) (velocityY * ParticleType.OPAQUE_PLACE.getLifeTimeTicks() * 0.05F);
        int sampleZ = zOffset + (int) (velocityZ * ParticleType.OPAQUE_PLACE.getLifeTimeTicks() * 0.05F);

        if (sampleX >= 0 && sampleX < lengthX
                && sampleY >= 0 && sampleY < lengthY
                && sampleZ >= 0 && sampleZ < lengthZ) {
            int bitIndex = MaterialsData.getUncompressedIndex(sampleX, sampleY, sampleZ);
            return (bitMap[bitIndex >> 6] & 1L << bitIndex) != 0;
        }
        return false;
    }

    /**
     * Performs pack offset.
     *
     * @param x                 X coordinate in local block coordinates
     * @param y                 Y coordinate in local block coordinates
     * @param z                 Z coordinate in local block coordinates
     * @param disableTimeScalar parameter
     * @param invertMotion      parameter
     * @return result
     */
    private static int packOffset(int x, int y, int z, boolean disableTimeScalar, boolean invertMotion) {
        int invertTimeScalarInt = disableTimeScalar ? 1 << 30 : 0;
        int invertMotionInt = invertMotion ? 1 << 31 : 0;
        return invertMotionInt | invertTimeScalarInt | (x + PARTICLE_OFFSET & 0x3FF) << 20 | (y + PARTICLE_OFFSET & 0x3FF) << 10 | z + PARTICLE_OFFSET & 0x3FF;
    }

    /**
     * Performs pack velocity gravity.
     *
     * @param velocityX X coordinate in local block coordinates
     * @param velocityY Y coordinate in local block coordinates
     * @param velocityZ Z coordinate in local block coordinates
     * @param gravity   Y coordinate in local block coordinates
     * @return result
     */
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

    /**
     * Performs pack rotation material.
     *
     * @param rotationSpeedX X coordinate in local block coordinates
     * @param rotationSpeedY Y coordinate in local block coordinates
     * @param material       parameter
     * @return result
     */
    private static int packRotationMaterial(float rotationSpeedX, float rotationSpeedY, byte material) {
        rotationSpeedX = Math.clamp(rotationSpeedX, 0.0F, 15.99F);
        rotationSpeedY = Math.clamp(rotationSpeedY, 0.0F, 15.99F);

        int packedRotationSpeedX = (int) (rotationSpeedX * ROTATION_PACKING_FACTOR) & 0xFF;
        int packedRotationSpeedY = (int) (rotationSpeedY * ROTATION_PACKING_FACTOR) & 0xFF;

        return Material.getProperties(material) << MeshGenerator.PROPERTIES_OFFSET | packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | material & 0xFF;
    }


    private final ArrayList<ParticleEffect> particleEffects = new ArrayList<>();
    private final ArrayList<ToBufferParticleEffect> toBufferParticleEffects = new ArrayList<>();
    private final Random random = new Random();

    private static final float VELOCITY_PACKING_FACTOR = 4.0F;  // Inverse in Particle.vert
    private static final float GRAVITY_PACKING_FACTOR = 2.0F;   // Inverse in Particle.vert
    private static final float ROTATION_PACKING_FACTOR = 16.0F; // Inverse in Particle.vert
    static final int PARTICLE_OFFSET = 512;                     // Same in Particle.vert

    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;
}
