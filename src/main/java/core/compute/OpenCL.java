package core.compute;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.FunctionProviderLocal;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opencl.CL11.*;
import static org.lwjgl.opencl.KHRICD.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public final class OpenCL {

    private OpenCL() {

    }

    public static void init() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer platformIDs = stack.mallocInt(1);
            IntBuffer errorCodeReturn = stack.callocInt(1);

            checkCLError(clGetPlatformIDs(null, platformIDs));
            PointerBuffer platforms = stack.mallocPointer(platformIDs.get(0));
            checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));
            if (platformIDs.get(0) == 0) throw new RuntimeException("No OpenCL platforms found.");

            PointerBuffer contextProperties = stack.mallocPointer(3);
            contextProperties.put(0, CL_CONTEXT_PLATFORM).put(2, 0);

            long platform = platforms.get(0);
            CLCapabilities platformCapabilities = CL.createPlatformCapabilities(platform);
            printPlatformInfo(platformCapabilities, contextProperties, platform);

            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, platformIDs));
            PointerBuffer devices = stack.mallocPointer(platformIDs.get(0));
            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices, (IntBuffer) null));

            device = devices.get(0);
            printDeviceInfo(device, platformCapabilities);

            CLContextCallback contextCB = CLContextCallback.create((errorInfo, _, _, _) -> {
                System.err.println("[LWJGL] cl_context_callback");
                System.err.println("\tInfo: " + memUTF8(errorInfo));
            });
            context = clCreateContext(contextProperties, device, contextCB, NULL, errorCodeReturn);
            checkCLError(errorCodeReturn);
        }
    }

    public static boolean checkCLError(IntBuffer errorCode) {
        return checkCLError(errorCode.get(errorCode.position()));
    }

    public static boolean checkCLError(int error) {
        if (error == CL_SUCCESS) return false;
        System.out.printf("Error %s at line %s%n", getErrorName(error), new Exception().getStackTrace()[1].toString());
        return true;
    }

    public static long createQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errorCodeReturn = stack.callocInt(1);
            long queue = clCreateCommandQueue(context, device, NULL, errorCodeReturn);
            if (checkCLError(errorCodeReturn)) throw new RuntimeException("Failed to create queue");
            return queue;
        }
    }

    public static void deleteQueue(long queue) {
        if (checkCLError(clReleaseCommandQueue(queue))) throw new RuntimeException("Failed to release command queue");
    }


    private static void printPlatformInfo(CLCapabilities platformCapabilities, PointerBuffer contextProperties, long platform) {
        contextProperties.put(1, platform);

        System.out.println("\n-------------------------");
        System.out.printf("CHOOSING PLATFORM: [0x%X]\n", platform);

        printPlatformInfo(platform, "CL_PLATFORM_PROFILE", CL_PLATFORM_PROFILE);
        printPlatformInfo(platform, "CL_PLATFORM_VERSION", CL_PLATFORM_VERSION);
        printPlatformInfo(platform, "CL_PLATFORM_NAME", CL_PLATFORM_NAME);
        printPlatformInfo(platform, "CL_PLATFORM_VENDOR", CL_PLATFORM_VENDOR);
        printPlatformInfo(platform, "CL_PLATFORM_EXTENSIONS", CL_PLATFORM_EXTENSIONS);
        if (platformCapabilities.cl_khr_icd) printPlatformInfo(platform, "CL_PLATFORM_ICD_SUFFIX_KHR", CL_PLATFORM_ICD_SUFFIX_KHR);
    }

    private static void printDeviceInfo(long device, CLCapabilities platformCapabilities) {
        CLCapabilities deviceCapabilities = CL.createDeviceCapabilities(device, platformCapabilities);

        System.out.printf("\n\tCHOOSING DEVICE: [0x%X]\n", device);
        System.out.println("\tCL_DEVICE_TYPE = " + getDeviceInfoLong(device, CL_DEVICE_TYPE));
        System.out.println("\tCL_DEVICE_VENDOR_ID = " + getDeviceInfoInt(device, CL_DEVICE_VENDOR_ID));
        System.out.println("\tCL_DEVICE_MAX_COMPUTE_UNITS = " + getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS));
        System.out.println("\tCL_DEVICE_MAX_WORK_ITEM_DIMENSIONS = " + getDeviceInfoInt(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS));
        System.out.println("\tCL_DEVICE_MAX_WORK_GROUP_SIZE = " + getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE));
        System.out.println("\tCL_DEVICE_MAX_CLOCK_FREQUENCY = " + getDeviceInfoInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY));
        System.out.println("\tCL_DEVICE_ADDRESS_BITS = " + getDeviceInfoInt(device, CL_DEVICE_ADDRESS_BITS));
        System.out.println("\tCL_DEVICE_AVAILABLE = " + (getDeviceInfoInt(device, CL_DEVICE_AVAILABLE) != 0));
        System.out.println("\tCL_DEVICE_COMPILER_AVAILABLE = " + (getDeviceInfoInt(device, CL_DEVICE_COMPILER_AVAILABLE) != 0));

        printDeviceInfo(device, "CL_DEVICE_NAME", CL_DEVICE_NAME);
        printDeviceInfo(device, "CL_DEVICE_VENDOR", CL_DEVICE_VENDOR);
        printDeviceInfo(device, "CL_DRIVER_VERSION", CL_DRIVER_VERSION);
        printDeviceInfo(device, "CL_DEVICE_PROFILE", CL_DEVICE_PROFILE);
        printDeviceInfo(device, "CL_DEVICE_VERSION", CL_DEVICE_VERSION);
        printDeviceInfo(device, "CL_DEVICE_EXTENSIONS", CL_DEVICE_EXTENSIONS);
        if (deviceCapabilities.OpenCL11) printDeviceInfo(device, "CL_DEVICE_OPENCL_C_VERSION", CL_DEVICE_OPENCL_C_VERSION);
    }

    public static void get(FunctionProviderLocal provider, long platform, String name) {
        System.out.println(name + ": " + provider.getFunctionAddress(platform, name));
    }

    private static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + getDeviceInfoStringUTF8(device, param));
    }

    private static String getEventStatusName(int status) {
        return switch (status) {
            case CL_QUEUED -> "CL_QUEUED";
            case CL_SUBMITTED -> "CL_SUBMITTED";
            case CL_RUNNING -> "CL_RUNNING";
            case CL_COMPLETE -> "CL_COMPLETE";
            default -> throw new IllegalArgumentException(String.format("Unsupported event status: 0x%X", status));
        };
    }

    private static String getPlatformInfoStringASCII(long cl_platform_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, buffer, null));

            return memASCII(buffer, bytes - 1);
        }
    }

    private static String getPlatformInfoStringUTF8(long cl_platform_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, buffer, null));

            return memUTF8(buffer, bytes - 1);
        }
    }

    private static int getDeviceInfoInt(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pl = stack.mallocInt(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    private static long getDeviceInfoLong(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pl = stack.mallocLong(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    private static long getDeviceInfoPointer(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pp, null));
            return pp.get(0);
        }
    }

    private static String getDeviceInfoStringUTF8(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, buffer, null));

            return memUTF8(buffer, bytes - 1);
        }
    }

    private static int getProgramBuildInfoInt(long cl_program_id, long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pl = stack.mallocInt(1);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    private static String getProgramBuildInfoStringASCII(long cl_program_id, long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, buffer, null));

            return memASCII(buffer, bytes - 1);
        }
    }

    private static String getErrorName(int error) {
        return switch (error) {
            case CL_SUCCESS -> "CL_SUCCESS";
            case CL_DEVICE_NOT_FOUND -> "CL_DEVICE_NOT_FOUND";
            case CL_DEVICE_NOT_AVAILABLE -> "CL_DEVICE_NOT_AVAILABLE";
            case CL_COMPILER_NOT_AVAILABLE -> "CL_COMPILER_NOT_AVAILABLE";
            case CL_MEM_OBJECT_ALLOCATION_FAILURE -> "CL_MEM_OBJECT_ALLOCATION_FAILURE";
            case CL_OUT_OF_RESOURCES -> "CL_OUT_OF_RESOURCES";
            case CL_OUT_OF_HOST_MEMORY -> "CL_OUT_OF_HOST_MEMORY";
            case CL_PROFILING_INFO_NOT_AVAILABLE -> "CL_PROFILING_INFO_NOT_AVAILABLE";
            case CL_MEM_COPY_OVERLAP -> "CL_MEM_COPY_OVERLAP";
            case CL_IMAGE_FORMAT_MISMATCH -> "CL_IMAGE_FORMAT_MISMATCH";
            case CL_IMAGE_FORMAT_NOT_SUPPORTED -> "CL_IMAGE_FORMAT_NOT_SUPPORTED";
            case CL_BUILD_PROGRAM_FAILURE -> "CL_BUILD_PROGRAM_FAILURE";
            case CL_MAP_FAILURE -> "CL_MAP_FAILURE";
            case CL_INVALID_VALUE -> "CL_INVALID_VALUE";
            case CL_INVALID_DEVICE_TYPE -> "CL_INVALID_DEVICE_TYPE";
            case CL_INVALID_PLATFORM -> "CL_INVALID_PLATFORM";
            case CL_INVALID_DEVICE -> "CL_INVALID_DEVICE";
            case CL_INVALID_CONTEXT -> "CL_INVALID_CONTEXT";
            case CL_INVALID_QUEUE_PROPERTIES -> "CL_INVALID_QUEUE_PROPERTIES";
            case CL_INVALID_COMMAND_QUEUE -> "CL_INVALID_COMMAND_QUEUE";
            case CL_INVALID_HOST_PTR -> "CL_INVALID_HOST_PTR";
            case CL_INVALID_MEM_OBJECT -> "CL_INVALID_MEM_OBJECT";
            case CL_INVALID_IMAGE_FORMAT_DESCRIPTOR -> "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR";
            case CL_INVALID_IMAGE_SIZE -> "CL_INVALID_IMAGE_SIZE";
            case CL_INVALID_SAMPLER -> "CL_INVALID_SAMPLER";
            case CL_INVALID_BINARY -> "CL_INVALID_BINARY";
            case CL_INVALID_BUILD_OPTIONS -> "CL_INVALID_BUILD_OPTIONS";
            case CL_INVALID_PROGRAM -> "CL_INVALID_PROGRAM";
            case CL_INVALID_PROGRAM_EXECUTABLE -> "CL_INVALID_PROGRAM_EXECUTABLE";
            case CL_INVALID_KERNEL_NAME -> "CL_INVALID_KERNEL_NAME";
            case CL_INVALID_KERNEL_DEFINITION -> "CL_INVALID_KERNEL_DEFINITION";
            case CL_INVALID_KERNEL -> "CL_INVALID_KERNEL";
            case CL_INVALID_ARG_INDEX -> "CL_INVALID_ARG_INDEX";
            case CL_INVALID_ARG_VALUE -> "CL_INVALID_ARG_VALUE";
            case CL_INVALID_ARG_SIZE -> "CL_INVALID_ARG_SIZE";
            case CL_INVALID_KERNEL_ARGS -> "CL_INVALID_KERNEL_ARGS";
            case CL_INVALID_WORK_DIMENSION -> "CL_INVALID_WORK_DIMENSION";
            case CL_INVALID_WORK_GROUP_SIZE -> "CL_INVALID_WORK_GROUP_SIZE";
            case CL_INVALID_WORK_ITEM_SIZE -> "CL_INVALID_WORK_ITEM_SIZE";
            case CL_INVALID_GLOBAL_OFFSET -> "CL_INVALID_GLOBAL_OFFSET";
            case CL_INVALID_EVENT_WAIT_LIST -> "CL_INVALID_EVENT_WAIT_LIST";
            case CL_INVALID_EVENT -> "CL_INVALID_EVENT";
            case CL_INVALID_OPERATION -> "CL_INVALID_OPERATION";
            case CL_INVALID_BUFFER_SIZE -> "CL_INVALID_BUFFER_SIZE";
            case CL_INVALID_GLOBAL_WORK_SIZE -> "CL_INVALID_GLOBAL_WORK_SIZE";
            default -> "Unrecognized error : " + error;
        };
    }


    private static long device;
    private static long context;
}