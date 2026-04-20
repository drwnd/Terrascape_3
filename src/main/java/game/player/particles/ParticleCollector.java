package game.player.particles;

import core.utils.IntArrayList;
import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.player.rendering.MeshGenerator;
import game.server.Game;
import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;
import game.server.material.Material;
import game.settings.OptionSettings;
import game.settings.ToggleSettings;

import java.util.ArrayList;
import java.util.Iterator;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class ParticleCollector {

    public static final int SHADER_PARTICLE_INT_SIZE = 3;

    public void uploadParticleEffects() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) particleEffects.add(loadParticleEffect(particleEffect));
        }
    }

    public void playParticleEffectSounds() {
        synchronized (toBufferParticleEffects) {
            for (ToBufferParticleEffect particleEffect : toBufferParticleEffects) ParticleType.playSound(particleEffect);
        }
    }

    public void clearToBufferParticleEffects() {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.clear();
        }
    }

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

    public void cleanUp() {
        synchronized (particleEffects) {
            for (ParticleEffect particleEffect : particleEffects) glDeleteBuffers(particleEffect.buffer());
            particleEffects.clear();
        }
    }

    public ArrayList<ParticleEffect> getParticleEffects() {
        return particleEffects;
    }

    public void addBreakPlaceParticleEffect(long startX, long startY, long startZ, int length, byte material, long[] bitMap) {
        if (!ToggleSettings.SHOW_BREAK_PARTICLES.value() && !ToggleSettings.SHOW_CUBE_PLACE_PARTICLES.value()) return;
        if (!ToggleSettings.SHOW_BREAK_PARTICLES.value()) {
            addPlaceParticleEffect(startX, startY, startZ, length, material, bitMap);
            return;
        }
        if (!ToggleSettings.SHOW_CUBE_PLACE_PARTICLES.value() || material == AIR) {
            addBreakParticleEffect(startX, startY, startZ, length, material, bitMap);
            return;
        }

        IntArrayList opaqueParticles = new IntArrayList(length * length * length);
        IntArrayList transparentParticles = new IntArrayList(length * length * length);
        IntArrayList placeParticles = new IntArrayList(length * length * length);
        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;

        for (int xOffset = 0; xOffset < length; xOffset++)
            for (int yOffset = 0; yOffset < length; yOffset++)
                for (int zOffset = 0; zOffset < length; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (previousMaterial == OUT_OF_WORLD || previousMaterial == material || paint && previousMaterial == AIR || replaceAir && previousMaterial != AIR)
                        continue;

                    add(placeParticles,
                            xOffset, yOffset, zOffset,
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);

                    if (previousMaterial != AIR)
                        add(Material.isGlass(previousMaterial) ? transparentParticles : opaqueParticles,
                                xOffset, yOffset, zOffset,
                                getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                                getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, previousMaterial,
                                false, false);
                }

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_BREAK);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_BREAK);
        addParticles(startX, startY, startZ, placeParticles, Material.isGlass(material) ? ParticleType.TRANSPARENT_PLACE : ParticleType.OPAQUE_PLACE);
    }

    public void addBreakPlaceParticleEffect(long startX, long startY, long startZ, int countX, int countY, int countZ, ShapePlaceable placeable) {
        boolean addBreakEffect = ToggleSettings.SHOW_BREAK_PARTICLES.value();
        boolean addPlaceEffect = ToggleSettings.SHOW_CUBE_PLACE_PARTICLES.value() && placeable.getMaterial() != AIR;
        if (!addBreakEffect && !addPlaceEffect) return;

        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();

        IntArrayList opaqueParticles = new IntArrayList(placeable.getPreferredSize() * countX * countY * countZ);
        IntArrayList transparentParticles = new IntArrayList(placeable.getPreferredSize() * countX * countY * countZ);
        IntArrayList placeParticles = new IntArrayList(placeable.getPreferredSize() * countX * countY * countZ);

        for (long x = startX; x < startX + (long) countX * lengthX; x += lengthX)
            for (long y = startY; y < startY + (long) countY * lengthY; y += lengthY)
                for (long z = startZ; z < startZ + (long) countZ * lengthZ; z += lengthZ) {
                    if (addBreakEffect && !addPlaceEffect)
                        addBreakEffectLoop(startX, startY, startZ, x, y, z, transparentParticles, opaqueParticles, placeable);
                    else if (addBreakEffect)
                        addBreakPlaceEffectLoop(startX, startY, startZ, x, y, z, placeParticles, transparentParticles, opaqueParticles, placeable);
                    else if (addPlaceEffect)
                        addPlaceEffectLoop(startX, startY, startZ, x, y, z, placeParticles, placeable);
                }

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_BREAK);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_BREAK);
        addParticles(startX, startY, startZ, placeParticles, Material.isGlass(placeable.getMaterial()) ? ParticleType.TRANSPARENT_PLACE : ParticleType.OPAQUE_PLACE);
    }

    public void addPlaceParticleEffect(long startX, long startY, long startZ, Structure structure) {
        if (!ToggleSettings.SHOW_STRUCTURE_PLACE_PARTICLES.value()) return;
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

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_BREAK);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_BREAK);
    }

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


    private void addBreakParticleEffect(long startX, long startY, long startZ, int length, byte ignoreMaterial, long[] bitMap) {
        if (!ToggleSettings.SHOW_BREAK_PARTICLES.value() || OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR) return;
        IntArrayList opaqueParticles = new IntArrayList(length * length * length);
        IntArrayList transparentParticles = new IntArrayList(length * length * length);

        for (int xOffset = 0; xOffset < length; xOffset++)
            for (int yOffset = 0; yOffset < length; yOffset++)
                for (int zOffset = 0; zOffset < length; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (previousMaterial == AIR || previousMaterial == OUT_OF_WORLD || previousMaterial == ignoreMaterial) continue;

                    add(Material.isGlass(previousMaterial) ? transparentParticles : opaqueParticles,
                            xOffset, yOffset, zOffset,
                            getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, previousMaterial,
                            false, false);
                }

        addParticles(startX, startY, startZ, opaqueParticles, ParticleType.OPAQUE_BREAK);
        addParticles(startX, startY, startZ, transparentParticles, ParticleType.TRANSPARENT_BREAK);
    }

    private void addPlaceParticleEffect(long startX, long startY, long startZ, int length, byte material, long[] bitMap) {
        if (!ToggleSettings.SHOW_CUBE_PLACE_PARTICLES.value() || material == AIR) return;
        IntArrayList particles = new IntArrayList(length * length * length);
        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;

        for (int xOffset = 0; xOffset < length; xOffset++)
            for (int yOffset = 0; yOffset < length; yOffset++)
                for (int zOffset = 0; zOffset < length; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(startX + xOffset, startY + yOffset, startZ + zOffset, 0);
                    if (previousMaterial == material || paint && previousMaterial == AIR || replaceAir && previousMaterial != AIR) continue;
                    add(particles,
                            xOffset, yOffset, zOffset,
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);
                }
        addParticles(startX, startY, startZ, particles, Material.isGlass(material) ? ParticleType.TRANSPARENT_PLACE : ParticleType.OPAQUE_PLACE);
    }

    private void addParticles(long startX, long startY, long startZ, IntArrayList particles, ParticleType type) {
        long currentTick = Game.getServer().getCurrentGameTick();
        if (particles.isEmpty()) return;
        int[] particlesData = packParticlesIntoBuffer(particles);
        ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, currentTick, type, startX, startY, startZ);
        addToBufferParticleEffect(effect);
    }

    private void addToBufferParticleEffect(ToBufferParticleEffect particleEffect) {
        synchronized (toBufferParticleEffects) {
            toBufferParticleEffects.add(particleEffect);
        }
    }

    private static void addBreakPlaceEffectLoop(long startX, long startY, long startZ,
                                                long x, long y, long z,
                                                IntArrayList placeParticles, IntArrayList transparentParticles, IntArrayList opaqueParticles, ShapePlaceable placeable) {
        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();
        long[] bitMap = placeable.getBitMap();
        byte material = placeable.getMaterial();

        for (int xOffset = 0; xOffset < lengthX; xOffset++)
            for (int yOffset = 0; yOffset < lengthY; yOffset++)
                for (int zOffset = 0; zOffset < lengthZ; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(x + xOffset, y + yOffset, z + zOffset, 0);
                    if (previousMaterial == OUT_OF_WORLD || previousMaterial == material || paint && previousMaterial == AIR || replaceAir && previousMaterial != AIR)
                        continue;

                    add(placeParticles,
                            xOffset + (int) (x - startX), yOffset + (int) (y - startY), zOffset + (int) (z - startZ),
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);

                    if (previousMaterial != AIR)
                        add(Material.isGlass(previousMaterial) ? transparentParticles : opaqueParticles,
                                xOffset + (int) (x - startX), yOffset + (int) (y - startY), zOffset + (int) (z - startZ),
                                getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                                getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, previousMaterial,
                                false, false);
                }
    }

    private static void addPlaceEffectLoop(long startX, long startY, long startZ,
                                           long x, long y, long z,
                                           IntArrayList placeParticles, ShapePlaceable placeable) {
        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();
        long[] bitMap = placeable.getBitMap();
        byte material = placeable.getMaterial();

        for (int xOffset = 0; xOffset < lengthX; xOffset++)
            for (int yOffset = 0; yOffset < lengthY; yOffset++)
                for (int zOffset = 0; zOffset < lengthZ; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(x + xOffset, y + yOffset, z + zOffset, 0);
                    if (previousMaterial == material || paint && previousMaterial == AIR || replaceAir && previousMaterial != AIR) continue;
                    add(placeParticles,
                            xOffset + (int) (x - startX), yOffset + (int) (y - startY), zOffset + (int) (z - startZ),
                            getRandom(-8F, 8F), getRandom(-8F, 8F), getRandom(-8F, 8F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), 0.0F, material,
                            true, true);
                }
    }

    private static void addBreakEffectLoop(long startX, long startY, long startZ,
                                           long x, long y, long z,
                                           IntArrayList transparentParticles, IntArrayList opaqueParticles, ShapePlaceable placeable) {
        if (OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR) return;
        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();
        long[] bitMap = placeable.getBitMap();
        byte material = placeable.getMaterial();

        for (int xOffset = 0; xOffset < lengthX; xOffset++)
            for (int yOffset = 0; yOffset < lengthY; yOffset++)
                for (int zOffset = 0; zOffset < lengthZ; zOffset++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(xOffset, yOffset, zOffset);
                    if ((bitMap[bitMapIndex >> 6] & 1L << bitMapIndex) == 0) continue;
                    byte previousMaterial = Game.getWorld().getMaterial(x + xOffset, y + yOffset, z + zOffset, 0);
                    if (previousMaterial == AIR || previousMaterial == OUT_OF_WORLD || previousMaterial == material) continue;

                    add(Material.isGlass(previousMaterial) ? transparentParticles : opaqueParticles,
                            xOffset + (int) (x - startX), yOffset + (int) (y - startY), zOffset + (int) (z - startZ),
                            getRandom(-12F, 12F), getRandom(-2F, 25F), getRandom(-12F, 12F),
                            getRandom(0.0F, 5F), getRandom(0.0F, 5F), BREAK_PARTICLE_GRAVITY, previousMaterial,
                            false, false);
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
        int particlesBuffer = glCreateBuffers();
        glNamedBufferData(particlesBuffer, particleEffect.particlesData(), GL_STATIC_DRAW);

        return new ParticleEffect(particlesBuffer, particleEffect.spawnTick(),
                particleEffect.type().getLifeTimeTicks(), particleEffect.particlesData().length / SHADER_PARTICLE_INT_SIZE,
                particleEffect.type().isOpaque(), particleEffect.x(), particleEffect.y(), particleEffect.z());
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

        return Material.getProperties(material) << MeshGenerator.PROPERTIES_OFFSET | packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | material & 0xFF;
    }


    private final ArrayList<ParticleEffect> particleEffects = new ArrayList<>();
    private final ArrayList<ToBufferParticleEffect> toBufferParticleEffects = new ArrayList<>();

    private static final float VELOCITY_PACKING_FACTOR = 4.0F;  // Inverse in Particle.vert
    private static final float GRAVITY_PACKING_FACTOR = 2.0F;   // Inverse in Particle.vert
    private static final float ROTATION_PACKING_FACTOR = 16.0F; // Inverse in Particle.vert
    static final int PARTICLE_OFFSET = 512;                     // Same in Particle.vert

    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;
}
