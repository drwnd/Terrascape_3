package core.rendering_api.shaders;

import core.assets.AssetManager;
import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.Debug;
import core.utils.FileManager;

import core.utils.MainThread;
import org.lwjgl.opengl.GL46;

public class ComputeShader extends Shader {

    @MainThread
    public ComputeShader(String computeShaderFilepath, ShaderIdentifier identifier) {
        this(FileManager.loadFileContents(AssetManager.getAssetFilepath(SHADER_FOLDER_PATH.resolve(computeShaderFilepath))), identifier.toString());
    }

    @MainThread
    public ComputeShader(String computeShaderCode, String shaderName) {
        try {
            programID = createProgram();
            int computeShaderID = createComputeShader(computeShaderCode, programID);
            link(programID, computeShaderID);

            Debug.log("Creating uniforms for Shader %s%n", shaderName);
            createUniforms(computeShaderCode, shaderName);

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @MainThread
    private static int createComputeShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL46.GL_COMPUTE_SHADER, programID);
    }

    @MainThread
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
