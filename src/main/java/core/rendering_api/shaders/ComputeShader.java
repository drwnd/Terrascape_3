package core.rendering_api.shaders;

import core.assets.identifiers.ShaderIdentifier;
import core.utils.FileManager;
import org.lwjgl.opengl.GL46;

public class ComputeShader extends Shader {

    public ComputeShader(String computeShaderFilePath, ShaderIdentifier identifier) {
        this(FileManager.loadFileContents(SHADER_FOLDER_PATH + computeShaderFilePath), identifier.toString());
    }

    public ComputeShader(String computeShaderCode, String name) {
        try {
            programID = createProgram();
            int computeShaderID = createComputeShader(computeShaderCode, programID);
            link(programID, computeShaderID);

            System.out.printf("Creating uniforms for Shader %s%n", name);
            createUniforms(computeShaderCode);

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static int createComputeShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL46.GL_COMPUTE_SHADER, programID);
    }

    private static void link(int programID, int computeShaderID) throws Exception {
        GL46.glLinkProgram(programID);

        if (GL46.glGetProgrami(programID, GL46.GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code: " + GL46.glGetProgramInfoLog(programID, 1024));

        if (computeShaderID != 0) GL46.glDetachShader(programID, computeShaderID);

        GL46.glValidateProgram(programID);
        if (GL46.glGetProgrami(programID, GL46.GL_VALIDATE_STATUS) == 0)
            throw new Exception("Unable to validate shader code: " + GL46.glGetProgramInfoLog(programID, 1024));
    }
}
