package core.rendering_api.shaders;

import core.assets.identifiers.ShaderIdentifier;
import core.utils.FileManager;
import org.lwjgl.opengl.GL46;

public class RenderShader extends Shader {

    public RenderShader(String vertexShaderFilepath, String fragmentShaderFilepath, ShaderIdentifier identifier) {
        String vertexShaderCode = FileManager.loadFileContents(vertexShaderFilepath);
        String fragmentShaderCode = FileManager.loadFileContents(fragmentShaderFilepath);

        int vertexShaderID = createVertexShader(vertexShaderCode, programID);
        int fragmentShaderID = createFragmentShader(fragmentShaderCode, programID);

        link(programID);

        if (vertexShaderID != 0) GL46.glDetachShader(programID, vertexShaderID);
        if (fragmentShaderID != 0) GL46.glDetachShader(programID, fragmentShaderID);

        System.out.printf("Creating uniforms for Shader %s%n", identifier);
        createUniforms(vertexShaderCode);
        createUniforms(fragmentShaderCode);
    }

    private static int createVertexShader(String shaderCode, int programID) {
        return createShader(shaderCode, GL46.GL_VERTEX_SHADER, programID);
    }

    private static int createFragmentShader(String shaderCode, int programID) {
        return createShader(shaderCode, GL46.GL_FRAGMENT_SHADER, programID);
    }
}
