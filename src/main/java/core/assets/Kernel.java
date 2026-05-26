package core.assets;

import core.compute.OpenCL;
import core.utils.FileManager;

import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL30.*;

public record Kernel(long id) implements Asset {

    public Kernel(String kernelFilePath) {
        this(getId(FileManager.loadFileContents("assets/shaders/" + kernelFilePath), kernelFilePath.substring(kernelFilePath.lastIndexOf('/'))));
    }

    public Kernel(String kernelCode, String kernelName) {
        this(getId(kernelCode, kernelName));
    }

    private static long getId(String kernelCode, String kernelName) {
        IntBuffer errorCodeReturn = IntBuffer.allocate(1);

        long program = clCreateProgramWithSource(OpenCL.context, kernelCode, errorCodeReturn);
        OpenCL.checkCLError(errorCodeReturn);
        OpenCL.checkCLError(clBuildProgram(program, OpenCL.device, "", null, 0));

        long kernelID = clCreateKernel(program, kernelName, errorCodeReturn);
        OpenCL.checkCLError(errorCodeReturn);
        return kernelID;
    }

    @Override
    public void delete() {
        clReleaseKernel(id);
    }
}
