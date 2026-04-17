package game.assets;

import core.assets.AssetLoader;
import core.assets.SoundCollection;
import core.assets.identifiers.SoundCollectionIdentifier;
import core.assets.identifiers.SoundIdentifier;
import core.sound.Sound;

import java.io.File;
import java.util.ArrayList;

public enum MaterialSounds implements SoundCollectionIdentifier {
    NONE(null, ""),

    DIG_STONE("dig", "stone"),
    DIG_WOOD("dig", "wood"),
    DIG_GLASS("dig", "355340__samgd14__glass-breaking"),
    DIG_GRASS("dig", "grass"),
    DIG_GRAVEL("dig", "gravel"),
    DIG_SAND("dig", "sand"),
    DIG_SNOW("dig", "snow"),
    DIG_FOLIAGE("dig", "vines"),
    DIG_ICE("dig", "420880__inspectorj__impact-ice-moderate"),
    DIG_CLOTH("dig", "cloth"),

    STEP_STONE("step", "stone",0.8F, 1),
    STEP_WOOD("step", "wood", 1.2F, 1),
    STEP_GLASS("step", "glass"),
    STEP_GRASS("step", "grass", 2, 1),
    STEP_GRAVEL("step", "gravel"),
    STEP_SAND("step", "sand"),
    STEP_SNOW("step", "snow"),
    STEP_FOLIAGE("block/vine", "climb"),
    STEP_CLOTH("step", "cloth"),
    STEP_DIRT("step", "682127__henkonen__footsteps-dirt-road-1"),

    JUMP_CLOTH("jump", "cloth"),
    JUMP_GRASS("jump", "grass"),
    JUMP_GRAVEL("jump", "gravel"),
    JUMP_SAND("jump", "sand"),
    JUMP_SNOW("jump", "snow"),
    JUMP_STONE("jump", "stone"),
    JUMP_WOOD("jump", "wood"),

    SWIM("random", "swim"),
    SPLASH("random", "splash"),
    POP("liquid", "lavapop");

    MaterialSounds(String folderPath, String fileNamePrefix) {
        this(folderPath, fileNamePrefix, 1.0F, 1.0F);
    }

    MaterialSounds(String folderPath, String fileNamePrefix, float gainMultiplier, float pitchMultiplier) {
        this.folderPath = folderPath;
        this.fileNamePrefix = fileNamePrefix;
        this.gainMultiplier = gainMultiplier;
        this.pitchMultiplier = pitchMultiplier;
    }

    public static MaterialSounds get(String name) {
        if (name == null) return NONE;
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception _) {
            return NONE;
        }
    }


    @Override
    public SoundCollection generateAsset() {
        if (folderPath == null) return new SoundCollection(new SingleSoundIdentifier[0]);
        File soundFolder = new File("assets/sounds/" + folderPath);
        if (!soundFolder.exists() || !soundFolder.isDirectory()) return new SoundCollection(new SingleSoundIdentifier[0]);

        File[] files = soundFolder.listFiles();
        if (files == null) return new SoundCollection(new SingleSoundIdentifier[0]);

        ArrayList<SoundIdentifier> identifiers = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().startsWith(fileNamePrefix)) continue;
            identifiers.add(new SingleSoundIdentifier(file.getPath(), gainMultiplier, pitchMultiplier));
        }

        return new SoundCollection(identifiers.toArray(new SoundIdentifier[0]));
    }

    private final String folderPath, fileNamePrefix;
    private final float gainMultiplier, pitchMultiplier;

    private record SingleSoundIdentifier(String filePath, float gainMultiplier, float pitchMultiplier) implements SoundIdentifier{
        @Override
        public Sound generateAsset() {
            return new Sound(AssetLoader.loadSound(filePath, false), gainMultiplier, pitchMultiplier);
        }
    }
}
