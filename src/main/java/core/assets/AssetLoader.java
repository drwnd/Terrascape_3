package core.assets;

import core.rendering_api.Debug;
import core.rendering_api.shaders.TextShader;

import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.openal.AL10.*;

public final class AssetLoader {

    private AssetLoader() {

    }

    //https://ahbejarano.gitbook.io/lwjglgamedev/chapter-16
    public static int loadSound(Path filepath) {
        int buffer = alGenBuffers();

        STBVorbisInfo info = STBVorbisInfo.malloc();
        ShortBuffer pcm = readVorbis(filepath, info);
        alBufferData(buffer, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
        info.free();
        MemoryUtil.memFree(pcm);

        return buffer;
    }

    public static Texture loadTexture2D(Path filepath) {
        int width, height;
        ByteBuffer buffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            buffer = STBImage.stbi_load(filepath.toString(), w, h, c, 4);
            if (buffer == null) {
                Debug.err("Image File %s not loaded %s%n", filepath, STBImage.stbi_failure_reason());
                return new Texture(0);
            }

            width = w.get();
            height = h.get();
        }

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        STBImage.stbi_image_free(buffer);
        return new Texture(id, width, height);
    }

    public static GuiElement loadGuiElement(GuiElementData data) {
        verifyGuiElementData(data);
        int vertexCount = data.getVertexCount(), vao = createVAO();
        float[][] floatAttributes = data.floatAttributes();
        int[][] intAttributes = data.intAttributes();
        int[] attributeSizes = data.attributeSizes();

        int[] vbos = new int[floatAttributes.length + intAttributes.length];
        for (int index = 0; index < floatAttributes.length; index++)
            vbos[index] = storeDataInAttributeList(index, attributeSizes[index], floatAttributes[index]);
        for (int index = floatAttributes.length; index < attributeSizes.length; index++)
            vbos[index] = storeDataInAttributeList(index, attributeSizes[index], intAttributes[index - floatAttributes.length]);

        glBindVertexArray(0);
        for (int vbo : vbos) {
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glDeleteBuffers(vbo);
        }

        return new GuiElement(vao, vertexCount);
    }

    public static int generateModelIndexBuffer(int quadCount) {
        int length = quadCount * 6;
        int[] indices = new int[length];
        int index = 0;
        for (int i = 0; i < length; i += 6) {
            indices[i + 0] = index + 0;
            indices[i + 1] = index + 1;
            indices[i + 2] = index + 2;
            indices[i + 3] = index + 3;
            indices[i + 4] = index + 2;
            indices[i + 5] = index + 1;
            index += 4;
        }
        int id = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        return id;
    }

    public static int generateTextRowVertexArray() {
        int vao = createVAO();

        final int offsetX = 256;
        final int offsetY = 512;

        int[] textData = new int[TextShader.MAX_TEXT_LENGTH * 4];
        for (int i = 0; i < TextShader.MAX_TEXT_LENGTH * 4; i += 4) {
            textData[i] = i >> 2;
            textData[i + 1] = i >> 2 | offsetX;
            textData[i + 2] = i >> 2 | offsetY;
            textData[i + 3] = i >> 2 | offsetX | offsetY;
        }
        int vbo = storeDataInAttributeList(0, 1, textData);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glDeleteBuffers(vbo);

        return vao;
    }


    public static int createVAO() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        return vao;
    }

    public static int storeDataInAttributeList(int attributeNo, int size, float[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNo, size, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static int storeDataInAttributeList(int attributeNo, int size, int[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribIPointer(attributeNo, size, GL_INT, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static void storeIndicesInBuffer(int[] indices) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = storeDataInIntBuffer(indices);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
    }

    public static IntBuffer storeDataInIntBuffer(int[] data) {
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();
        return buffer;
    }

    //https://ahbejarano.gitbook.io/lwjglgamedev/chapter-16
    private static ShortBuffer readVorbis(Path filepath, STBVorbisInfo info) throws RuntimeException {
        long decoder;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            // IDE has no idea what it's talking about
            decoder = STBVorbis.stb_vorbis_open_filename(filepath.toString(), error, null);
            if (decoder == MemoryUtil.NULL) throw new RuntimeException("Failed to open Ogg Vorbis file " + filepath + ". Error: " + error.get(0));
        }

        STBVorbis.stb_vorbis_get_info(decoder, info);

        int channels = info.channels();
        int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
        ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);

        result.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);
        STBVorbis.stb_vorbis_close(decoder);

        return result;
    }

    private static void verifyGuiElementData(GuiElementData data) {
        if (data == null || data.floatAttributes() == null || data.intAttributes() == null || data.attributeSizes() == null) throw new NullPointerException();
        float[][] floatAttributes = data.floatAttributes();
        int[][] intAttributes = data.intAttributes();
        int[] attributeSizes = data.attributeSizes();

        if (floatAttributes.length + intAttributes.length != attributeSizes.length)
            throw new IllegalArgumentException("Specify same number of attributes and attribute sizes");

        int vertexCount = data.getVertexCount();
        for (int attributeSize : attributeSizes) if (attributeSize <= 0) throw new IllegalArgumentException("Sizes must be strictly positive integers");

        for (int index = 0; index < floatAttributes.length; index++) {
            float[] attribute = floatAttributes[index];
            if (attribute == null) throw new IllegalArgumentException("An attribute cannot be null");
            if (attribute.length / attributeSizes[index] != vertexCount || attribute.length % attributeSizes[index] != 0)
                throw new IllegalArgumentException("Inconsistent vertex count");
        }

        for (int index = 0; index < intAttributes.length; index++) {
            int[] attribute = intAttributes[index];
            if (attribute == null) throw new IllegalArgumentException("An attribute cannot be null");
            if (attribute.length / attributeSizes[floatAttributes.length + index] != vertexCount || attribute.length % attributeSizes[floatAttributes.length + index] != 0)
                throw new IllegalArgumentException("Inconsistent vertex count");
        }
    }
}
