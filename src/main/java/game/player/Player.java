package game.player;

import core.rendering_api.Window;
import core.settings.KeySetting;
import core.settings.ToggleSetting;

import game.player.interaction.InteractionHandler;
import game.player.interaction.Placeable;
import game.player.movement.Movement;
import game.player.rendering.Camera;
import game.player.rendering.MeshCollector;
import game.player.rendering.Renderer;
import game.server.Game;
import game.server.material.Material;
import game.utils.Position;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class Player {

    public Player(Position position) {
        meshCollector = new MeshCollector();
        camera = new Camera();
        input = new PlayerInput();
        movement = new Movement();
        renderer = new Renderer();
        interactionHandler = new InteractionHandler();
        hotbar = new Hotbar();
        inventory = new Inventory();

        renderer.addRenderable(hotbar);
        renderer.addRenderable(inventory);
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
            Vector3f movementThisTick = movement.getRenderVelocity().mul(fraction - 1);
            Position toRenderPosition = new Position(position);
            toRenderPosition.add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
            toRenderPosition.add(0, movement.getState().getCameraElevation(), 0);
            camera.setPosition(toRenderPosition);
        }
    }

    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, camera.getRotation());
        }
        if (canDoActiveActions()) interactionHandler.updateGameTick();
    }

    /**
     * Intended for actions that should not be taken when a menu is displayed.
     * For example movement, block interactions etc.
     */
    public void handleActiveButtonInput(int button, int action) {
        movement.handleInput(button, action);
        interactionHandler.handleInput(button, action);
        hotbar.handleInput(button, action);
    }

    /**
     * Intended for actions that could always be taken.
     * For example Closing a menu or toggling the debug screen.
     */
    public void handleInactiveKeyInput(int button, int action) {
        if (button == KeySetting.ZOOM.value() && action != GLFW.GLFW_REPEAT) camera.setZoomed(action == GLFW.GLFW_PRESS);
        if (button == KeySetting.INVENTORY.value() && action == GLFW.GLFW_PRESS) toggleInventory();

        if (button == KeySetting.DEBUG_MENU.value() && action == GLFW.GLFW_PRESS) renderer.toggleDebugScreen();
        if (button == KeySetting.RELOAD_MATERIALS.value() && action == GLFW.GLFW_PRESS) Material.loadMaterials();
        if (button == KeySetting.NO_CLIP.value() && action == GLFW.GLFW_PRESS) noClip = !noClip;
    }

    public void handleInactiveScrollInput(double xScroll, double yScroll) {
        if (camera.isZoomed()) {
            final float zoomFactorChange = 0.9f;
            camera.changeZoom(yScroll > 0 ? zoomFactorChange : 1 / zoomFactorChange);
            return;
        }

        if (ToggleSetting.SCROLL_HOTBAR.value()) {
            Hotbar hotbar = Game.getPlayer().getHotbar();
            hotbar.setSelectedSlot(hotbar.getSelectedSlot() + (yScroll < 0.0 ? 1 : -1));
        }
    }


    public Placeable getHeldPlaceable() {
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

    public void setInput() {
        Window.setInput(input);
    }

    public boolean canDoActiveActions() {
        return !inventory.isVisible();
    }

    public boolean isNoClip() {
        return noClip;
    }

    public void cleanUp() {

    }

    void toggleInventory() {
        inventory.setVisible(!inventory.isVisible());
        Window.setInput(inventory.isVisible() ? new InventoryInput(inventory) : input);
        if (inventory.isVisible()) inventory.updateDisplayPositions();
    }

    private final MeshCollector meshCollector;
    private final Camera camera;
    private final PlayerInput input;
    private final Movement movement;
    private final Renderer renderer;
    private final InteractionHandler interactionHandler;
    private final Hotbar hotbar;
    private final Inventory inventory;

    private boolean noClip = false;
    private Position position; // Center of the players feet
}
