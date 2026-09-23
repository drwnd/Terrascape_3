package core.rendering_api;

import core.settings.CoreOptionSettings;
import core.settings.optionSettings.LogMessages;
import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public final class Debug {

    @MainThread
    public static void checkError(String lastAction) {
        int error = glGetError();
        System.out.println(lastAction + " : " + getErrorString(error));
    }

    public static String debugMessageToString(int source, int type, int id, int severity, String message) {
        return "Source: %s\r\nType: %s\r\nId: %d\r\nSeverity: %s\r\n%s".formatted(
                getSourceString(source),
                getTypeString(type),
                id,
                getSeverityString(severity),
                message
        );
    }


    @MainThread
    public static void clearErrors() {
        while (glGetError() != GL_NO_ERROR) ;
    }

    @MainThread
    public static int getError() {
        return glGetError();
    }

    public static void log(String message) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        System.out.println(message);
    }

    public static void log(String format, Object... args) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        System.out.printf(format, args);
    }

    public static void log(Object object) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        System.out.println(object);
    }

    public static void log(Exception exception) {
        if (CoreOptionSettings.LOG_MESSAGES.value() != LogMessages.ALL) return;
        exception.printStackTrace(System.out);
    }

    public static void err(String message) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        System.err.println(message);
    }

    public static void err(String format, Object... args) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        System.err.printf(format, args);
    }

    public static void err(Object object) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        System.err.println(object);
    }

    public static void err(Exception exception) {
        if (CoreOptionSettings.LOG_MESSAGES.value() == LogMessages.NONE) return;
        exception.printStackTrace(System.err);
    }


    private static String getErrorString(int error) {
        return switch (error) {
            case GL_NO_ERROR -> "No error";
            case GL_INVALID_ENUM -> "Invalid Enum";
            case GL_INVALID_VALUE -> "Invalid Value";
            case GL_INVALID_OPERATION -> "Invalid Operation";
            case GL_STACK_OVERFLOW -> "Stack Overflow";
            case GL_STACK_UNDERFLOW -> "Stack Underflow";
            case GL_OUT_OF_MEMORY -> "Out of Memory";
            case GL_INVALID_FRAMEBUFFER_OPERATION -> "Invalid Framebuffer Operation";
            default -> "Unknown 0x" + Integer.toHexString(error).toUpperCase();
        };
    }

    private static String getSourceString(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            default -> "Unknown 0x" + Integer.toHexString(source).toUpperCase();
        };
    }

    private static String getTypeString(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_POP_GROUP -> "Pop Group";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            default -> "Unknown 0x" + Integer.toHexString(type).toUpperCase();
        };
    }

    private static String getSeverityString(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> "Unknown 0x" + Integer.toHexString(severity).toUpperCase();
        };
    }
}
