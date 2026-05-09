package core.rendering_api;

import core.settings.CoreOptionSettings;
import core.settings.optionSettings.LogMessages;

import static org.lwjgl.opengl.GL46.*;

public final class Debug {

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

    public static void clearErrors() {
        while (glGetError() != GL_NO_ERROR) ;
    }

    public static void log(String message) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        System.out.println(message);
    }

    public static void log(String format, Object... args) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        System.out.printf(format, args);
    }

    public static void err(String message) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        System.err.println(message);
    }

    public static void err(String format, Object... args) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        System.err.printf(format, args);
    }
}
