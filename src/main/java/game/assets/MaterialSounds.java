package game.assets;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.SoundCollection;
import core.assets.identifiers.SoundCollectionIdentifier;
import core.assets.identifiers.SoundIdentifier;
import core.sound.Sound;
import core.utils.MainThread;

import java.nio.file.Path;
import java.util.ArrayList;

public final class MaterialSounds implements SoundCollectionIdentifier {

    public static String print(MaterialSounds sound) {
        if (sound == null) return "null";
        return "path: %s, prefix: %s, gain: %f, pitch: %f".formatted(sound.folderPath, sound.fileNamePrefix, sound.gainMultiplier, sound.pitchMultiplier);
    }

    @Override
    public SoundCollection generateAsset() {
        if (folderPath == null || fileNamePrefix == null) return new SoundCollection(new SingleSoundIdentifier[0]);
        ArrayList<Path> filePaths = AssetManager.getAssetFilePathsInFolderMatching(folderPath, fileNamePrefix);

        ArrayList<SoundIdentifier> identifiers = new ArrayList<>();
        for (Path filepath : filePaths) identifiers.add(new SingleSoundIdentifier(filepath, gainMultiplier, pitchMultiplier));

        return new SoundCollection(identifiers.toArray(new SoundIdentifier[0]));
    }

    @SuppressWarnings({"FieldMayBeFinal"})
    private String folderPath = null, fileNamePrefix = null;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private float gainMultiplier = 1.0F, pitchMultiplier = 1.0F;

    private record SingleSoundIdentifier(Path filepath, float gainMultiplier, float pitchMultiplier) implements SoundIdentifier {
        @Override
        @MainThread
        public Sound generateAsset() {
            return new Sound(AssetLoader.loadSound(filepath), gainMultiplier, pitchMultiplier);
        }
    }
}
