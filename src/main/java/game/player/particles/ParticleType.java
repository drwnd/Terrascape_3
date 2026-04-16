package game.player.particles;

import core.sound.Sound;
import game.server.material.Material;
import game.settings.FloatSettings;
import game.utils.Position;

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
        long[] involvedMaterials = getInvolvedMaterials(toBufferParticleEffect);
        Position position = new Position(toBufferParticleEffect.x(), toBufferParticleEffect.y(), toBufferParticleEffect.z(), 0.0F, 0.0F, 0.0F);

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            if ((involvedMaterials[material >> 6] & 1L << material) == 0) continue;
            Sound.play3D(Material.getDigSounds((byte) material), FloatSettings.DIG_AUDIO, position, null);
        }
    }

    private static void playPlaceEffectSounds(ToBufferParticleEffect toBufferParticleEffect) {
        long[] involvedMaterials = getInvolvedMaterials(toBufferParticleEffect);
        Position position = new Position(toBufferParticleEffect.x(), toBufferParticleEffect.y(), toBufferParticleEffect.z(), 0.0F, 0.0F, 0.0F);

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            if ((involvedMaterials[material >> 6] & 1L << material) == 0) continue;
            Sound.play3D(Material.getStepSounds((byte) material), FloatSettings.PLACE_AUDIO, position, null);
        }
    }

    private static void playSplashEffectSounds(ToBufferParticleEffect toBufferParticleEffect) {

    }

    private static long[] getInvolvedMaterials(ToBufferParticleEffect toBufferParticleEffect) {
        long[] involvedMaterials = new long[4];
        int[] particleData = toBufferParticleEffect.particlesData();

        for (int index = 2; index < particleData.length; index += 3) {
            int material = particleData[index] & 0xFF;
            involvedMaterials[material >> 6] |= 1L << material;
        }
        return involvedMaterials;
    }


    private final int lifeTimeTicks;
    private final boolean isOpaque;
}
