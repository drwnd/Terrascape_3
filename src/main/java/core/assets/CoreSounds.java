package core.assets;

import core.assets.identifiers.SoundIdentifier;
import core.sound.Sound;

public enum CoreSounds implements SoundIdentifier {
    CLICK("note/bd.ogg", 1, 1, 0, 0);

    CoreSounds(String filePath, float gainMultiplier, float pitchMultiplier, float gainWiggle, float pitchWiggle) {
        this.filePath = filePath;
        this.gainMultiplier = gainMultiplier;
        this.pitchMultiplier = pitchMultiplier;
        this.gainWiggle = gainWiggle;
        this.pitchWiggle = pitchWiggle;
    }

    @Override
    public Sound generateAsset() {
        return new Sound(AssetLoader.loadSound(filePath), gainMultiplier, pitchMultiplier, gainWiggle, pitchWiggle);
    }

    private final String filePath;
    private final float gainWiggle, pitchWiggle;
    private final float gainMultiplier, pitchMultiplier;
}
