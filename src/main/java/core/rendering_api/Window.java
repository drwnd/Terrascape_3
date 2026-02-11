package core.rendering_api;

import core.renderables.Renderable;
import core.settings.FloatSetting;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Window {

    private Window() {
    }

    public static void init(String title) {
        Window.maximized = true;

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        createWindow(title);
        GL.createCapabilities();

        glClearColor(0, 0, 0, 1);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        glClearDepth(0.0F);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    private static void createWindow(String title) {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode == null) throw new RuntimeException("Could not get video mode");

        if (maximized) {
            window = glfwCreateWindow(vidMode.width(), vidMode.height(), title, glfwGetPrimaryMonitor(), MemoryUtil.NULL);
            width = vidMode.width();
            height = vidMode.height();
        } else {
            width = vidMode.width() / 2;
            height = vidMode.height() / 2;
            window = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
            glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        if (window == MemoryUtil.NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwSetFramebufferSizeCallback(window, (long _, int width, int height) -> {
            Window.width = width;
            Window.height = height;
            Vector2i size = new Vector2i(width, height);
            for (Renderable renderable : renderablesStack) renderable.resize(size, 1.0F, 1.0F);
        });

        glfwMakeContextCurrent(window);
        glfwShowWindow(window);
        glfwSwapInterval(1);
    }

    public static void renderLoop() {
        while (!glfwWindowShouldClose(window)) {
            try {
                long start = System.nanoTime();

                glViewport(0, 0, width, height);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
                Renderable renderable = renderablesStack.getLast();
                renderable.render(new Vector2f(0.0F, 0.0F), new Vector2f(1.0F, 1.0F));

                frameTime = (long) (frameTime * 0.975 + (System.nanoTime() - start) * 0.025);

                glfwSwapBuffers(window);
                glfwPollEvents();

            } catch (Exception exception) {
                CrashAction action = crashCallback.notify(exception);
                switch (action) {
                    case PRINT -> exception.printStackTrace();
                    case CLOSE -> glfwSetWindowShouldClose(window, true);
                    case THROW -> throw exception;
                    case PRINT_AND_CLOSE -> {
                        exception.printStackTrace();
                        glfwSetWindowShouldClose(window, true);
                    }
                }
            }
        }
    }

    public static void toggleFullScreen() {
        maximized = !maximized;
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode == null) throw new RuntimeException("Could not get video mode");

        if (maximized) glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, vidMode.width(), vidMode.height(), GLFW_DONT_CARE);
        else glfwSetWindowMonitor(window, MemoryUtil.NULL, width / 4, height / 4, width / 2, height / 2, GLFW_DONT_CARE);
    }

    public static void cleanUp() {
        glfwDestroyWindow(window);
    }

    public static Vector2f toPixelCoordinate(Vector2f position, boolean scalesWithGuiSize) {
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0F;
        return position.mul(guiSize).add((1 - guiSize) * 0.5F, (1 - guiSize) * 0.5F).mul(Window.getWidth(), Window.getHeight());
    }

    public static Vector2f toPixelSize(Vector2f size, boolean scalesWithGuiSize) {
        float guiSize = scalesWithGuiSize ? FloatSetting.GUI_SIZE.value() : 1.0F;
        return size.mul(Window.getWidth(), Window.getHeight()).mul(guiSize);
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static float getAspectRatio() {
        return (float) width / height;
    }

    public static long getWindow() {
        return window;
    }

    public static long getCPUFrameTime() {
        return frameTime;
    }

    public static void pushRenderable(Renderable element) {
        renderablesStack.add(element);
        element.setOnTop();
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    public static void popRenderable() {
        renderablesStack.removeLast().delete();
        if (renderablesStack.isEmpty()) glfwSetWindowShouldClose(window, true);
        else renderablesStack.getLast().setOnTop();
    }

    public static Renderable topRenderable() {
        return renderablesStack.getLast();
    }

    public static Input getInput() {
        return input;
    }

    public static void setInput(Input input) {
        Window.input.unset();
        Window.input = input;
        input.setInputMode();
        glfwSetCursorPosCallback(window, (long window, double xPos, double yPos) -> {
            standardInput.cursorPosCallback(window, xPos, yPos);
            input.cursorPosCallback(window, xPos, yPos);
        });
        glfwSetMouseButtonCallback(window, (long window, int button, int action, int mods) -> {
            standardInput.mouseButtonCallback(window, button, action, mods);
            input.mouseButtonCallback(window, button, action, mods);
        });
        glfwSetScrollCallback(window, (long window, double xScroll, double yScroll) -> {
            standardInput.scrollCallback(window, xScroll, yScroll);
            input.scrollCallback(window, xScroll, yScroll);
        });
        glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
            standardInput.keyCallback(window, key, scancode, action, mods);
            input.keyCallback(window, key, scancode, action, mods);
        });
        glfwSetCharCallback(window, (long window, int codePoint) -> {
            standardInput.charCallback(window, codePoint);
            input.charCallback(window, codePoint);
        });
    }


    public static void setCrashCallback(CrashCallback crashCallback) {
        Window.crashCallback = crashCallback;
    }

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

    private static final ArrayList<Renderable> renderablesStack = new ArrayList<>();
    private static final StandardWindowInput standardInput = new StandardWindowInput();

    private static int width, height;
    private static long window;
    private static boolean maximized;
    private static long frameTime = 0L;

    private static Input input = standardInput;
    private static CrashCallback crashCallback = _ -> CrashAction.PRINT;
}
