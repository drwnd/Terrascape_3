package core.assets;

import core.assets.identifiers.GuiElementIdentifier;
import core.assets.identifiers.TextureIdentifier;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class AssetLoader {

    private AssetLoader() {

    }


    public static Texture loadTexture(TextureIdentifier identifier) {
        int width, height;
        ByteBuffer buffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            String filepath = identifier.getFilepath();
            buffer = STBImage.stbi_load(filepath, w, h, c, 4);
            if (buffer == null) throw new RuntimeException("Image FIle " + filepath + " not loaded " + STBImage.stbi_failure_reason());

            width = w.get();
            height = h.get();
        }

        int id = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id);
        GL46.glPixelStorei(GL46.GL_UNPACK_ALIGNMENT, 1);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, width, height, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, buffer);
        GL46.glGenerateMipmap(GL46.GL_TEXTURE_2D);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        STBImage.stbi_image_free(buffer);
        return new Texture(id);
    }

    public static GuiElement loadGuiElement(GuiElementIdentifier identifier) {
        int vao = createVAO();
        int vbo1 = storeDateInAttributeList(0, 2, identifier.vertices);
        int vbo2 = storeDateInAttributeList(1, 2, identifier.textureCoordinates);

        GL46.glBindVertexArray(0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo1);
        GL46.glDeleteBuffers(vbo1);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo2);
        GL46.glDeleteBuffers(vbo2);

        return new GuiElement(vao, identifier.vertices.length);
    }

    public static int createVAO() {
        int vao = GL46.glGenVertexArrays();
        GL46.glBindVertexArray(vao);
        return vao;
    }

    public static int storeDateInAttributeList(int attributeNo, int size, float[] data) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, data, GL46.GL_STATIC_DRAW);
        GL46.glVertexAttribPointer(attributeNo, size, GL46.GL_FLOAT, false, 0, 0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static int storeDateInAttributeList(int[] data) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, data, GL46.GL_STATIC_DRAW);
        GL46.glVertexAttribIPointer(0, 1, GL46.GL_INT, 0, 0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static void storeIndicesInBuffer(int[] indices) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = storeDateInIntBuffer(indices);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, buffer, GL46.GL_STATIC_DRAW);
    }

    public static IntBuffer storeDateInIntBuffer(int[] data) {
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();
        return buffer;
    }
}
