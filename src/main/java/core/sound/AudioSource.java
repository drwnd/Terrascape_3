package core.sound;

import core.settings.CoreFloatSettings;

import static org.lwjgl.openal.AL10.*;

final class AudioSource {

    AudioSource() {
        source = alGenSources();
        alSourcef(source, AL_GAIN, CoreFloatSettings.MASTER_AUDIO.value());
        alSourcef(source, AL_PITCH, 1);
    }

    AudioSource setPosition(float x, float y, float z) {
        alSource3f(source, AL_POSITION, x, y, z);
        return this;
    }

    AudioSource setVelocity(float x, float y, float z) {
        alSource3f(source, AL_VELOCITY, x, y, z);
        return this;
    }

    AudioSource setGain(float gain) {
        alSourcef(source, AL_GAIN, CoreFloatSettings.MASTER_AUDIO.value() * gain);
        return this;
    }

    AudioSource setPitch(float pitch) {
        alSourcef(source, AL_PITCH, pitch);
        return this;
    }

    void play(int buffer) {
        alSourcei(source, AL_BUFFER, buffer);
        alSourcePlay(source);
    }

    boolean isPlaying() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    void delete() {
        alDeleteSources(source);
    }

    private final int source;
}

