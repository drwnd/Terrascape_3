package core.sound;

import core.assets.Asset;
import core.assets.AssetManager;
import core.assets.SoundCollection;
import core.assets.identifiers.SoundCollectionIdentifier;
import core.assets.identifiers.SoundIdentifier;
import core.settings.CoreFloatSettings;
import core.settings.FloatSetting;

import game.utils.Distanceable;
import org.joml.Vector3f;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public record Sound(int buffer, float gainMultiplier, float pitchMultiplier) implements Asset {

    public static void init() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        if (defaultDeviceName == null) throw new RuntimeException("Could not get default audio device");
        device = alcOpenDevice(defaultDeviceName);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(alcCapabilities);

        alListener3f(AL_POSITION, 0.0F, 0.0F, 0.0F);
        alListener3f(AL_VELOCITY, 0.0F, 0.0F, 0.0F);
        alListenerfv(AL_ORIENTATION, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F});

        for (int index = 0; index < sources.length; index++) sources[index] = new AudioSource();
    }

    public static void setDistanceScaler(float distanceScaler) {
        Sound.distanceScaler = distanceScaler;
    }

    public static void setListenerData(Distanceable distanceable, Vector3f direction, Vector3f velocity) {
        Sound.distanceable = distanceable;
        Sound.velocity = velocity;
        alListenerfv(AL_ORIENTATION, new float[]{direction.x, direction.y, direction.z, 0.0F, 1.0F, 0.0F});
    }

    public static void cleanUp() {
        for (AudioSource source : sources) source.delete();
        alcMakeContextCurrent(MemoryUtil.NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    public static int getConcurrentlyPlayedSounds() {
        int counter = 0;
        for (AudioSource source : sources) if (source.isPlaying()) counter++;
        return counter;
    }

    public static void playUI(SoundCollectionIdentifier identifier, FloatSetting gain) {
        SoundCollection sounds = AssetManager.get(identifier);
        if (sounds.identifiers().length == 0) return;
        SoundIdentifier singleIdentifier = sounds.identifiers()[(int) (Math.random() * sounds.identifiers().length)];
        playUI(singleIdentifier, gain, 1.0F, 1.0F);
    }

    public static void playUI(SoundIdentifier identifier, FloatSetting gain) {
        playUI(identifier, gain, 1.0F, 1.0F);
    }

    public static void playUI(SoundIdentifier identifier, FloatSetting gain, float gainMultiplier, float pitchMultiplier) {
        AudioSource source = getNextFreeAudioSource();
        if (source == null) return;

        gainMultiplier *= gain == null || gain == CoreFloatSettings.MASTER_AUDIO ? 1.0F : gain.value();
        Sound sound = AssetManager.get(identifier);

        source.setPosition(0, 0, 0).setVelocity(0, 0, 0);
        source.setGain(sound.gainMultiplier * gainMultiplier).setPitch(sound.pitchMultiplier * pitchMultiplier).play(sound.buffer);
    }

    public static void play3D(SoundCollectionIdentifier identifier, FloatSetting gain, Distanceable distanceable, Vector3f velocity) {
        SoundCollection sounds = AssetManager.get(identifier);
        if (sounds.identifiers().length == 0) return;
        SoundIdentifier singleIdentifier = sounds.identifiers()[(int) (Math.random() * sounds.identifiers().length)];
        play3D(singleIdentifier, gain, distanceable, velocity);
    }

    public static void play3D(SoundIdentifier identifier, FloatSetting gain, Distanceable distanceable, Vector3f velocity) {
        AudioSource source = getNextFreeAudioSource();
        if (source == null) return;

        float gainMultiplier = gain == null || gain == CoreFloatSettings.MASTER_AUDIO ? 1.0F : gain.value();
        Sound sound = AssetManager.get(identifier);

        Vector3f distance = distanceable.vectorFrom(Sound.distanceable);
        velocity = velocity == null ? new Vector3f(Sound.velocity).negate() : new Vector3f(velocity).sub(Sound.velocity);

        source.setPosition(distance.x * distanceScaler, distance.y * distanceScaler, distance.z * distanceScaler);
        source.setVelocity(velocity.x * distanceScaler, velocity.y * distanceScaler, velocity.z * distanceScaler);
        source.setGain(sound.gainMultiplier * gainMultiplier).setPitch(sound.pitchMultiplier).play(sound.buffer);
    }


    private static AudioSource getNextFreeAudioSource() {
        for (AudioSource source : sources) if (!source.isPlaying()) return source;
        return null;
    }

    @Override
    public void delete() {
        alDeleteBuffers(buffer);
    }

    private static float distanceScaler = 1.0F;

    private static final AudioSource[] sources = new AudioSource[128];
    private static long device, context;
    private static Distanceable distanceable;
    private static Vector3f velocity;
}
