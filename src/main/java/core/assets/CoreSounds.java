package core.assets;

import core.assets.identifiers.SoundIdentifier;
import core.sound.Sound;

public enum CoreSounds implements SoundIdentifier {
    BUTTON_SUCCESS("core/button_success.ogg", 1, 1),
    BUTTON_FAILURE("core/button_failure.ogg", 1, 1);

    CoreSounds(String filePath, float gainMultiplier, float pitchMultiplier) {
        this.filePath = filePath;
        this.gainMultiplier = gainMultiplier;
        this.pitchMultiplier = pitchMultiplier;
    }

    @Override
    public Sound generateAsset() {
        return new Sound(AssetLoader.loadSound(filePath, true), gainMultiplier, pitchMultiplier);
    }

    private final String filePath;
    private final float gainMultiplier, pitchMultiplier;
}
