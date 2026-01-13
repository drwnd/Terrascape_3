package core.rendering_api.shaders;

import core.assets.Asset;
import core.assets.identifiers.ShaderIdentifier;
import core.utils.FileManager;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL46.*;

public class Shader extends Asset {

    public Shader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        uniforms = new HashMap<>();

        String vertexShaderCode = FileManager.loadFileContents(SHADER_FOLDER_PATH + vertexShaderFilePath);
        String fragmentShaderCode = FileManager.loadFileContents(SHADER_FOLDER_PATH + fragmentShaderFilePath);
        try {
            programID = createProgram();
            int vertexShaderID = createVertexShader(vertexShaderCode, programID);
            int fragmentShaderID = createFragmentShader(fragmentShaderCode, programID);
            link(programID, vertexShaderID, fragmentShaderID);

            System.out.printf("Creating uniforms for Shader %s%n", identifier);
            createUniforms(vertexShaderCode);
            createUniforms(fragmentShaderCode);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }


    public void bind() {
        glUseProgram(programID);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(uniforms.get(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, int[] data) {
        glUniform1iv(uniforms.get(uniformName), data);
    }

    public void setUniform(String uniformName, int x, int y) {
        glUniform2i(uniforms.get(uniformName), x, y);
    }

    public void setUniform(String uniformName, Vector2i value) {
        glUniform2i(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3i value) {
        glUniform3i(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, int x, int y, int z) {
        glUniform3i(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, int x, int y, int z, int w) {
        glUniform4i(uniforms.get(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, float x, float y, float z) {
        glUniform3f(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, float x, float y, float z, float w) {
        glUniform4f(uniforms.get(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, float value) {
        glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Vector2f value) {
        glUniform2f(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, float x, float y) {
        glUniform2f(uniforms.get(uniformName), x, y);
    }

    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, boolean value) {
        glUniform1i(uniforms.get(uniformName), value ? 1 : 0);
    }

    public void setUniform(String uniformName, Color color) {
        glUniform3f(uniforms.get(uniformName), color.getRed() * 0.003921569f, color.getGreen() * 0.003921569f, color.getBlue() * 0.003921569f);
    }

    @Override
    public void delete() {
        uniforms.clear();
        if (programID != 0) glDeleteProgram(programID);
    }


    private static int createProgram() throws Exception {
        int programID = glCreateProgram();
        if (programID == 0) throw new Exception("Could not create Shader");
        return programID;
    }

    private static int createVertexShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL_VERTEX_SHADER, programID);
    }

    private static int createFragmentShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL_FRAGMENT_SHADER, programID);
    }

    private static void link(int programID, int vertexShaderID, int fragmentShaderID) throws Exception {
        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code: " + glGetProgramInfoLog(programID, 1024));

        if (vertexShaderID != 0) glDetachShader(programID, vertexShaderID);
        if (fragmentShaderID != 0) glDetachShader(programID, fragmentShaderID);
    }

    private static int createShader(String shaderCode, int shaderType, int programID) throws Exception {
        int shaderID = glCreateShader(shaderType);
        if (shaderID == 0) throw new Exception("Error creating shader. Type: " + shaderType);

        glShaderSource(shaderID, shaderCode);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader code: Type: " + shaderType + "Info: " + glGetShaderInfoLog(shaderID, 1024));

        glAttachShader(programID, shaderID);

        return shaderID;
    }

    private void createUniforms(String shaderCode) {
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
    private static final String SHADER_FOLDER_PATH = "assets/shaders/";

    protected final int programID;
    private final HashMap<String, Integer> uniforms;
}
