package core.rendering_api.shaders;

import core.assets.identifiers.ShaderIdentifier;
import core.utils.FileManager;
import org.lwjgl.opengl.GL46;

public class ComputeShader extends Shader {

    public ComputeShader(String computeShaderFilepath, ShaderIdentifier identifier) {
        String computeShaderCode = FileManager.loadFileContents(computeShaderFilepath);

        int computeShaderID = createComputeShader(computeShaderCode, programID);

        link(programID);

        if (computeShaderID != 0) GL46.glDetachShader(programID, computeShaderID);

        System.out.printf("Creating uniforms for Shader %s%n", identifier);
        createUniforms(computeShaderCode);
    }

    private static int createComputeShader(String shaderCode, int programID) {
        return createShader(shaderCode, GL46.GL_COMPUTE_SHADER, programID);
    }
}
