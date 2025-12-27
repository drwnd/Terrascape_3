package core.rendering_api.shaders;

import core.assets.Asset;

import org.joml.*;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class Shader extends Asset {

    public Shader() {
        uniforms = new HashMap<>();
        programID = createProgram();
    }


    public void bind() {
        GL46.glUseProgram(programID);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL46.glUniformMatrix4fv(uniforms.get(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, int[] data) {
        GL46.glUniform1iv(uniforms.get(uniformName), data);
    }

    public void setUniform(String uniformName, int x, int y) {
        GL46.glUniform2i(uniforms.get(uniformName), x, y);
    }

    public void setUniform(String uniformName, Vector2i value) {
        GL46.glUniform2i(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3i value) {
        GL46.glUniform3i(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, int x, int y, int z) {
        GL46.glUniform3i(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, int x, int y, int z, int w) {
        GL46.glUniform4i(uniforms.get(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, float x, float y, float z) {
        GL46.glUniform3f(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, float value) {
        GL46.glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Vector2f value) {
        GL46.glUniform2f(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, float x, float y) {
        GL46.glUniform2f(uniforms.get(uniformName), x, y);
    }

    public void setUniform(String uniformName, Vector3f value) {
        GL46.glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, int value) {
        GL46.glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, boolean value) {
        GL46.glUniform1i(uniforms.get(uniformName), value ? 1 : 0);
    }

    public void setUniform(String uniformName, Color color) {
        GL46.glUniform3f(uniforms.get(uniformName), color.getRed() * 0.003921569f, color.getGreen() * 0.003921569f, color.getBlue() * 0.003921569f);
    }

    @Override
    public void delete() {
        uniforms.clear();
        if (programID != 0) GL46.glDeleteProgram(programID);
    }


    static int createProgram() {
        int programID = GL46.glCreateProgram();
        if (programID == 0) throw new RuntimeException("Could not create Shader");
        return programID;
    }

    static void link(int programID) {
        GL46.glLinkProgram(programID);

        if (GL46.glGetProgrami(programID, GL46.GL_LINK_STATUS) == 0)
            throw new RuntimeException("Error linking shader code: " + GL46.glGetProgramInfoLog(programID, 1024));
    }

    static int createShader(String shaderCode, int shaderType, int programID) {
        int shaderID = GL46.glCreateShader(shaderType);
        if (shaderID == 0) throw new RuntimeException("Error creating shader. Type: " + shaderType);

        GL46.glShaderSource(shaderID, shaderCode);
        GL46.glCompileShader(shaderID);

        if (GL46.glGetShaderi(shaderID, GL46.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Error compiling shader code: Type: " + shaderType + "Info: " + GL46.glGetShaderInfoLog(shaderID, 1024));

        GL46.glAttachShader(programID, shaderID);

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
        int uniformLocation = GL46.glGetUniformLocation(programID, uniformName);
        if (uniformLocation == -1) System.err.println("Could not find uniform " + uniformName);
        uniforms.put(uniformName, uniformLocation);
        System.out.printf("-Created uniform %s with binding %s%n", uniformName, uniformLocation);
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

    protected final int programID;
    private final HashMap<String, Integer> uniforms;
}
