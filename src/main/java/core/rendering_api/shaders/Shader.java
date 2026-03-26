package core.rendering_api.shaders;

import core.assets.Asset;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL46.*;

public abstract class Shader implements Asset {

    static final String SHADER_FOLDER_PATH = "assets/shaders/";


    public void bind() {
        glUseProgram(programID);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(getUniform(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, int[] data) {
        glUniform1iv(getUniform(uniformName), data);
    }

    public void setUniform(String uniformName, float[] data) {
        glUniform1fv(getUniform(uniformName), data);
    }

    public void setUniform(String uniformName, Color color) {
        glUniform3f(getUniform(uniformName), color.getRed() * 0.003921569F, color.getGreen() * 0.003921569F, color.getBlue() * 0.003921569F);
    }

    public void setUniform(String uniformName, boolean value) {
        glUniform1i(getUniform(uniformName), value ? 1 : 0);
    }


    public void setUniform(String uniformName, int value) {
        glUniform1i(getUniform(uniformName), value);
    }

    public void setUniform(String uniformName, int x, int y) {
        glUniform2i(getUniform(uniformName), x, y);
    }

    public void setUniform(String uniformName, int x, int y, int z) {
        glUniform3i(getUniform(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, int x, int y, int z, int w) {
        glUniform4i(getUniform(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, Vector2i value) {
        glUniform2i(getUniform(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3i value) {
        glUniform3i(getUniform(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector4i value) {
        glUniform4i(getUniform(uniformName), value.x, value.y, value.z, value.w);
    }

    public void setUniform(String uniformName, long x, long y, long z) {
        glUniform3i(getUniform(uniformName), (int) x, (int) y, (int) z);
    }


    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniform(uniformName), value);
    }

    public void setUniform(String uniformName, float x, float y) {
        glUniform2f(getUniform(uniformName), x, y);
    }

    public void setUniform(String uniformName, float x, float y, float z) {
        glUniform3f(getUniform(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, float x, float y, float z, float w) {
        glUniform4f(getUniform(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, Vector2f value) {
        glUniform2f(getUniform(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(getUniform(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(getUniform(uniformName), value.x, value.y, value.z, value.w);
    }


    @Override
    public void delete() {
        uniforms.clear();
        if (programID != 0) glDeleteProgram(programID);
    }


    static int createProgram() throws Exception {
        int programID = glCreateProgram();
        if (programID == 0) throw new Exception("Could not create Shader");
        return programID;
    }

    static int createShader(String shaderCode, int shaderType, int programID) throws Exception {
        int shaderID = glCreateShader(shaderType);
        if (shaderID == 0) throw new Exception("Error creating shader. Type: " + shaderType);

        glShaderSource(shaderID, shaderCode);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader code: Type: " + shaderType + "Info: " + glGetShaderInfoLog(shaderID, 1024));

        glAttachShader(programID, shaderID);

        return shaderID;
    }

    void createUniforms(String shaderCode) {
        String[] lines = shaderCode.split("\n");

        for (String line : lines) {
            String stripped = strip(line);
            if (!stripped.startsWith("uniform")) continue;

            String uniformName = stripped.split(" ")[2];
            createUniform(uniformName);
        }
    }

    private void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        if (uniformLocation == -1) System.err.println("Could not find uniform " + uniformName);
        uniforms.put(uniformName, uniformLocation);
        System.out.printf("-Created uniform %s with binding %s%n", uniformName, uniformLocation);
    }

    private int getUniform(String uniformName) {
        Integer location = uniforms.get(uniformName);
        return location == null ? -1 : location;
    }

    private static String strip(String line) {
        line = line.strip();                                                           // Remove accidental white space
        line = REMOVE_SEMICOLON.matcher(line).replaceAll("");               // Remove trailing semicolon
        line = REMOVE_ARRAY_DECLARATION.matcher(line).replaceAll("");       // Remove Array declarations (C-Style ones would cause problems)
        line = REMOVE_UNNECESSARY_WHITESPACE.matcher(line).replaceAll(" "); // Remove unnecessary white space
        return line;
    }

    private static final Pattern REMOVE_SEMICOLON = Pattern.compile(";");
    private static final Pattern REMOVE_ARRAY_DECLARATION = Pattern.compile("\\[.*]");
    private static final Pattern REMOVE_UNNECESSARY_WHITESPACE = Pattern.compile(" +");

    protected int programID;
    private final HashMap<String, Integer> uniforms = new HashMap<>();
}
