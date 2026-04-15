package core.sound;

import core.assets.Asset;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;

public record Sound(int buffer) implements Asset {

    public static void init() {
        String defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        if (defaultDeviceName == null) throw new RuntimeException("Could not get default audio device");
        device = ALC10.alcOpenDevice(defaultDeviceName);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(alcCapabilities);

        for (int index = 0; index < sources.length; index++) sources[index] = new AudioSource();
    }

    public static void cleanUp() {
        for (AudioSource source : sources) source.delete();
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
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
