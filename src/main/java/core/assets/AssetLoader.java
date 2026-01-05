package core.assets;

import core.assets.identifiers.GuiElementIdentifier;
import core.rendering_api.shaders.TextShader;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public final class AssetLoader {

    private AssetLoader() {

    }


    public static Texture loadTexture2D(String filepath) {
        int width, height;
        ByteBuffer buffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            buffer = STBImage.stbi_load(filepath, w, h, c, 4);
            if (buffer == null) {
                System.err.println("Image File " + filepath + " not loaded " + STBImage.stbi_failure_reason());
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

    public static GuiElement loadGuiElement(GuiElementIdentifier identifier) {
        int vao = createVAO();
        int vbo1 = storeDateInAttributeList(0, 2, identifier.vertices());
        int vbo2 = storeDateInAttributeList(1, 2, identifier.textureCoordinates());

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo1);
        glDeleteBuffers(vbo1);
        glBindBuffer(GL_ARRAY_BUFFER, vbo2);
        glDeleteBuffers(vbo2);

        return new GuiElement(vao, identifier.vertices().length);
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
        int vbo = storeDateInAttributeList(textData);

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

    public static int storeDateInAttributeList(int attributeNo, int size, float[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNo, size, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static int storeDateInAttributeList(int[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribIPointer(0, 1, GL_INT, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static void storeIndicesInBuffer(int[] indices) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = storeDateInIntBuffer(indices);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
    }

    public static IntBuffer storeDateInIntBuffer(int[] data) {
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();
        return buffer;
    }
}
