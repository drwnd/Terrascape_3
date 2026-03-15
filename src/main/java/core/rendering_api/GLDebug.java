package core.rendering_api;

import static org.lwjgl.opengl.GL46.*;

public final class GLDebug {

    public static void checkError(String lastAction) {
        int error = glGetError();
        System.out.println(lastAction + " : " + switch (error) {
            case GL_NO_ERROR -> "No error";
            case GL_INVALID_ENUM -> "Invalid Enum";
            case GL_INVALID_VALUE -> "Invalid Value";
            case GL_INVALID_OPERATION -> "Invalid Operation";
            case GL_STACK_OVERFLOW -> "Stack Overflow";
            case GL_STACK_UNDERFLOW -> "Stack Underflow";
            case GL_OUT_OF_MEMORY -> "Out of Memory";
            default -> "Unknown 0x" + Integer.toHexString(error).toUpperCase();
        });
    }

    public static void clearOldErrors() {
        while (glGetError() != GL_NO_ERROR) ;
    }
}
