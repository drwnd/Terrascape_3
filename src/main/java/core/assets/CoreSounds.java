package core.assets;

import core.assets.identifiers.SoundIdentifier;
import core.sound.Sound;
import core.utils.MainThread;

import java.nio.file.Path;

public enum CoreSounds implements SoundIdentifier {
    BUTTON_SUCCESS(Path.of("sounds", "core", "button_success.ogg"), 1, 1),
    BUTTON_FAILURE(Path.of("sounds", "core", "button_failure.ogg"), 1, 1);

    CoreSounds(Path filePath, float gainMultiplier, float pitchMultiplier) {
        this.filepath = filePath;
        this.gainMultiplier = gainMultiplier;
        this.pitchMultiplier = pitchMultiplier;
    }

    @Override
    @MainThread
    public Sound generateAsset() {
        return new Sound(AssetLoader.loadSound(AssetManager.getAssetFilepath(filepath)), gainMultiplier, pitchMultiplier);
    }

    private final Path filepath;
    private final float gainMultiplier, pitchMultiplier;
}
