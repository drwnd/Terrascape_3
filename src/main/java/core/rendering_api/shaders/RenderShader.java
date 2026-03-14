package core.rendering_api.shaders;

import core.assets.identifiers.ShaderIdentifier;
import core.utils.FileManager;

import static org.lwjgl.opengl.GL20.*;

public class RenderShader extends Shader {

    public RenderShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {

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
}
