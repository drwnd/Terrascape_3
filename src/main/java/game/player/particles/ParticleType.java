package game.player.particles;

import core.sound.Sound;
import game.server.material.Material;
import game.settings.FloatSettings;
import game.utils.Position;
import game.utils.Utils;
import org.joml.Vector3i;

import static game.player.particles.ParticleCollector.*;
import static game.utils.Constants.*;

enum ParticleType {

    OPAQUE_BREAK(40, true),
    OPAQUE_PLACE(10, true),
    TRANSPARENT_BREAK(40, false),
    TRANSPARENT_PLACE(10, false),
    SPLASH(20, true);

    ParticleType(int lifeTimeTicks, boolean isOpaque) {
        this.lifeTimeTicks = lifeTimeTicks;
        this.isOpaque = isOpaque;
    }

    public static void playSound(ToBufferParticleEffect toBufferParticleEffect) {
        switch (toBufferParticleEffect.type()) {
            case SPLASH -> playSplashEffectSounds(toBufferParticleEffect);
            case OPAQUE_BREAK, TRANSPARENT_BREAK -> playBreakEffectSounds(toBufferParticleEffect);
            case OPAQUE_PLACE, TRANSPARENT_PLACE -> playPlaceEffectSounds(toBufferParticleEffect);
        }
    }

    public int getLifeTimeTicks() {
        return lifeTimeTicks;
    }

    public boolean isOpaque() {
        return isOpaque;
    }


    private static void playBreakEffectSounds(ToBufferParticleEffect toBufferParticleEffect) {
        long[] involvedMaterials = new long[4];
        Position center = new Position();
        computeInvolvedMaterialsAndCenter(toBufferParticleEffect, involvedMaterials, center);

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            if ((involvedMaterials[material >> 6] & 1L << material) == 0) continue;
            Sound.play3D(Material.getDigSounds((byte) material), FloatSettings.DIG_AUDIO, center, null);
        }
    }

    private static void playPlaceEffectSounds(ToBufferParticleEffect toBufferParticleEffect) {
        long[] involvedMaterials = new long[4];
        Position center = new Position();
        computeInvolvedMaterialsAndCenter(toBufferParticleEffect, involvedMaterials, center);

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            if ((involvedMaterials[material >> 6] & 1L << material) == 0) continue;
            Sound.play3D(Material.getStepSounds((byte) material), FloatSettings.PLACE_AUDIO, center, null);
        }
    }

    private static void playSplashEffectSounds(ToBufferParticleEffect toBufferParticleEffect) {

    }

    private static void computeInvolvedMaterialsAndCenter(ToBufferParticleEffect toBufferParticleEffect, long[] involvedMaterials, Position center) {
        int[] particleData = toBufferParticleEffect.particlesData();
        Vector3i min = new Vector3i(Integer.MAX_VALUE), max = new Vector3i(Integer.MIN_VALUE);

        for (int index = 0; index < particleData.length; index += 3) {
            int position = particleData[index];
            Utils.min(min, position >> 20 & 0x3FF, position >> 10 & 0x3FF, position & 0x3FF);
            Utils.max(max, position >> 20 & 0x3FF, position >> 10 & 0x3FF, position & 0x3FF);

            int material = particleData[index + 2] & 0xFF;
            involvedMaterials[material >> 6] |= 1L << material;
        }

        center.longX = toBufferParticleEffect.x() + (min.x + max.x >> 1) - PARTICLE_OFFSET;
        center.longY = toBufferParticleEffect.y() + (min.y + max.y >> 1) - PARTICLE_OFFSET;
        center.longZ = toBufferParticleEffect.z() + (min.z + max.z >> 1) - PARTICLE_OFFSET;
    }


    private final int lifeTimeTicks;
    private final boolean isOpaque;
}
