package core.sound;

import core.assets.Asset;
import core.assets.AssetManager;
import core.assets.identifiers.SoundIdentifier;
import core.settings.CoreFloatSettings;
import core.settings.FloatSetting;

import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public record Sound(int buffer, float gainMultiplier, float pitchMultiplier, float gainWiggle, float pitchWiggle) implements Asset {

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

    public static void cleanUp() {
        for (AudioSource source : sources) source.delete();
        alcMakeContextCurrent(MemoryUtil.NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
    
    public static void playUI(SoundIdentifier identifier, FloatSetting gain) {
        AudioSource source = getNextFreeAudioSource();
        if (source == null) return;
        
        float gainMultiplier = gain == null || gain == CoreFloatSettings.MASTER_AUDIO ? 1.0F : gain.value();
        Sound sound = AssetManager.get(identifier);
        
        source.setPosition(0, 0, 0).setVelocity(0, 0, 0);
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

    private static final AudioSource[] sources = new AudioSource[128];
    private static long device, context;
}
