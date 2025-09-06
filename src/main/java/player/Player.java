package player;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import player.interaction.InteractionHandler;
import player.movement.Movement;
import player.rendering.Camera;
import player.rendering.MeshCollector;
import player.rendering.Renderer;
import rendering_api.Window;
import server.Game;
import settings.KeySetting;
import utils.Position;

public final class Player {

    public Player(Position position) {
        meshCollector = new MeshCollector();
        camera = new Camera();
        input = new PlayerInput();
        movement = new Movement(new Vector3f());
        renderer = new Renderer();
        interactionHandler = new InteractionHandler();
        hotbar = new Hotbar();

        renderer.addRenderable(hotbar);
        this.position = position;
        Window.pushRenderable(renderer);
        setInput();
    }


    public void updateFrame() {
        meshCollector.uploadAllMeshes();
        meshCollector.deleteOldMeshes();

        float fraction = Game.getServer().getCurrentGameTickFraction();
        fraction = Math.clamp(fraction, 0.0f, 1.0f);

        synchronized (this) {
            camera.rotate(input.getCursorMovement());
            Vector3f movementThisTick = movement.getVelocity().mul(fraction);
            Position toRenderPosition = new Position(position);
            toRenderPosition.add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
            camera.setPlayerPositon(toRenderPosition);
        }
    }

    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, camera.getRotation());
        }
        interactionHandler.updateGameTick();
    }

    public void handleInput(int button, int action) {
        movement.handleInput(button, action);
        interactionHandler.handleInput(button, action);
        hotbar.handleInput(button, action);
        if (button == KeySetting.DEBUG_MENU.value() && action == GLFW.GLFW_PRESS) renderer.toggleDebugScreen();
    }


    public byte getHeldMaterial() {
        return hotbar.getSelectedMaterial();
    }

    public MeshCollector getMeshCollector() {
        return meshCollector;
    }

    public Camera getCamera() {
        return camera;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Position getPosition() {
        synchronized (this) {
            return new Position(position);
        }
    }

    public void setInput() {
        Window.setInput(input);
    }

    public void cleanUp() {

    }

    private final MeshCollector meshCollector;
    private final Camera camera;
    private final PlayerInput input;
    private final Movement movement;
    private final Renderer renderer;
    private final InteractionHandler interactionHandler;
    private final Hotbar hotbar;

    private Position position;
}
