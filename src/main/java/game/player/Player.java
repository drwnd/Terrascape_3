package game.player;

import core.rendering_api.Window;
import core.sound.Sound;
import core.utils.Vector3l;

import game.player.interaction.*;
import game.player.inventory.Inventory;
import game.player.movement.Movement;
import game.player.rendering.Camera;
import game.player.rendering.MeshCollector;
import game.player.particles.ParticleCollector;
import game.player.rendering.Renderer;
import game.server.Game;
import game.server.material.Material;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import game.utils.Position;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Player {

    /**
     * Initializes the player at the specified position.
     *
     * @param position the initial position of the player
     */
    public Player(Position position) {
        meshCollector = new MeshCollector();
        particleCollector = new ParticleCollector();
        camera = new Camera();
        input = new PlayerInput(this);
        movement = new Movement();
        renderer = new Renderer(this, meshCollector);
        interactionHandler = new InteractionHandler();
        hotbar = new Hotbar();
        inventory = new Inventory();
        chat = new ChatTextField();

        renderer.addHUDRenderable(hotbar);
        renderer.addRenderable(inventory);
        renderer.addRenderable(chat);
        this.position = position;
        Window.pushRenderable(renderer);
    }


    /**
     * Updates the player's state for each frame, including camera rotation and rendering position.
     */
    public void updateFrame() {
        Sound.setListenerData(camera.getPosition(), camera.getDirection(), movement.getVelocity());
        particleCollector.unloadParticleEffects();
        particleCollector.uploadParticleEffects();
        particleCollector.playParticleEffectSounds();
        particleCollector.clearToBufferParticleEffects();
        meshCollector.uploadAllMeshes();
        meshCollector.deleteOldMeshes();

        float fraction = Game.getServer().getCurrentGameTickFraction();
        fraction = Math.clamp(fraction, 0.0F, 1.0F);

        synchronized (this) {
            camera.rotate(input.getCursorMovement());
            Vector3f movementThisTick = movement.getRenderVelocity().mul(fraction - 1.0F);
            Position toRenderPosition = new Position(position)
                    .add(movementThisTick.x, movementThisTick.y, movementThisTick.z)
                    .addComponent(Y_COMPONENT, movement.getState().getCameraElevation());
            camera.setPosition(camera.applyPerspectiveOffset(toRenderPosition));
        }
    }

    /**
     * Updates the player's state for each game tick, including movement and interaction handling.
     */
    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, camera.getRotation());
        }
        if (canDoActiveActions()) interactionHandler.updateGameTick();
        renderer.updateGameTick();
    }

    /**
     * Updates the mesh collector and renderer when the render distance changes.
     *
     * @param oldRenderDistance the previous render distance in chunks
     */
    public void updateRenderDistance(int oldRenderDistance) {
        meshCollector = new MeshCollector(meshCollector, oldRenderDistance);
        renderer.reloadRenderingOptimizer();
    }

    /**
     * Updates the mesh collector and renderer when the LOD count changes.
     */
    public void updateLodCount() {
        meshCollector = new MeshCollector(meshCollector);
        renderer.reloadRenderingOptimizer();
    }

    /**
     * Intended for actions that should not be taken when a menu is displayed.
     * For example movement, block interactions etc.
     */
    public void handleActiveButtonInput(int button, int action) {
        movement.handleInput(button, action);
        interactionHandler.handleActiveInput(button, action);
        hotbar.handleInput(button, action);
        if (button == KeySettings.ROTATE_SHAPE_FORWARD.keybind() && action == GLFW_PRESS && getHeldPlaceable() != null) {
            renderer.invalidateHologram();
            getHeldPlaceable().rotateForwards();
        }
        if (button == KeySettings.ROTATE_SHAPE_BACKWARD.keybind() && action == GLFW_PRESS && getHeldPlaceable() != null) {
            renderer.invalidateHologram();
            getHeldPlaceable().rotateBackwards();
        }
    }

    /**
     * Intended for actions that could always be taken.
     * For example Closing a menu or toggling the debug screen.
     */
    public void handleInactiveKeyInput(int button, int action) {
        InteractionHandler.handleInactiveInput(button, action);

        if (button == KeySettings.ZOOM.keybind() && action != GLFW_REPEAT) camera.setZoomed(action == GLFW_PRESS);
        if (button == KeySettings.OPEN_INVENTORY.keybind() && action == GLFW_PRESS) toggleInventory();
        if (button == KeySettings.OPEN_CHAT.keybind() && action == GLFW_PRESS) toggleChat();
        if (button == KeySettings.START_COMMAND.keybind() && action == GLFW_PRESS) startCommand();

        if (button == KeySettings.RELOAD_MATERIALS.keybind() && action == GLFW_PRESS) Material.loadMaterials();
    }

    /**
     * Handles scroll input, used for zooming the camera, adjusting interaction states, or switching hotbar slots.
     *
     * @param yScroll the amount of scroll
     */
    public void handleScrollInput(double yScroll) {
        if (camera.isZoomed()) {
            final float zoomFactorChange = 0.9F;
            camera.changeZoom(yScroll > 0 ? zoomFactorChange : 1 / zoomFactorChange);
            return;
        }
        if (interactionHandler.getState(Target.getPlayerTarget()).isLocked()) {
            interactionHandler.handleScroll(yScroll);
            return;
        }

        if (ToggleSettings.SCROLL_HOTBAR.value()) hotbar.setSelectedSlot(hotbar.getSelectedSlot() + (yScroll < 0.0 ? 1 : -1));
    }


    /**
     * Calculates the minimum world block coordinates of the player's hitbox.
     *
     * @return the minimum world block coordinates (LOD 0)
     */
    public Vector3l getMinCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Position(this.position).add(-hitboxSize.x * 0.5F, 0.0F, -hitboxSize.z * 0.5F).longPosition();
    }

    /**
     * Calculates the maximum world block coordinates of the player's hitbox.
     *
     * @return the maximum world block coordinates (LOD 0)
     */
    public Vector3l getMaxCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Position(this.position).add(hitboxSize.x * 0.5F, hitboxSize.y, hitboxSize.z * 0.5F).longPosition();
    }

    public Placeable getHeldPlaceable() {
        return hotbar.getSelectedMaterial();
    }

    public MeshCollector getMeshCollector() {
        return meshCollector;
    }

    public ParticleCollector getParticleCollector() {
        return particleCollector;
    }

    public Camera getCamera() {
        return camera;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Movement getMovement() {
        return movement;
    }

    public Position getPosition() {
        synchronized (this) {
            return new Position(position);
        }
    }

    public Hotbar getHotbar() {
        return hotbar;
    }

    public InteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the input mode based on whether menus are visible.
     */
    public void setInput() {
        if (inventory.isVisible()) Window.setInput(inventory.getInput());
        else if (chat.isVisible()) Window.setInput(chat.getInput());
        else Window.setInput(input);
    }

    public void setPosition(Position position) {
        synchronized (this) {
            this.position = new Position(position);
        }
    }

    public boolean canDoActiveActions() {
        return !inventory.isVisible() && !chat.isVisible();
    }

    public boolean isChatOpen() {
        return chat.isVisible();
    }

    /**
     * Cleans up the player's resources.
     */
    public void cleanUp() {
        meshCollector.cleanUp();
        particleCollector.cleanUp();
    }

    /**
     * Starts the command input mode.
     */
    void startCommand() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        chat.setText("/");
        setInput();
    }

    /**
     * Toggles the chat visibility.
     */
    void toggleChat() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        setInput();
    }

    /**
     * Toggles the inventory visibility.
     */
    public void toggleInventory() {
        if (chat.isVisible()) return;
        inventory.setVisible(!inventory.isVisible());
        setInput();
    }

    private MeshCollector meshCollector;
    private final ParticleCollector particleCollector;
    private final Camera camera;
    private final PlayerInput input;
    private final Movement movement;
    private final Renderer renderer;
    private final InteractionHandler interactionHandler;
    private final Hotbar hotbar;
    private final Inventory inventory;
    private final ChatTextField chat;

    private Position position; // Center of the players feet
}
