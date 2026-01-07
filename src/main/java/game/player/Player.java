package game.player;

import core.rendering_api.Window;
import core.settings.KeySetting;
import core.settings.ToggleSetting;

import game.player.interaction.ChunkRebuildPlaceable;
import game.player.interaction.InteractionHandler;
import game.player.interaction.Placeable;
import game.player.movement.Movement;
import game.player.rendering.Camera;
import game.player.rendering.MeshCollector;
import game.player.rendering.ParticleCollector;
import game.player.rendering.Renderer;
import game.server.Game;
import game.server.material.Material;
import game.utils.Position;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Player {

    public Player(Position position) {
        meshCollector = new MeshCollector();
        particleCollector = new ParticleCollector();
        camera = new Camera();
        input = new PlayerInput();
        movement = new Movement();
        renderer = new Renderer(this);
        interactionHandler = new InteractionHandler();
        hotbar = new Hotbar();
        inventory = new Inventory();
        chat = new ChatTextField();

        renderer.addRenderable(hotbar);
        renderer.addRenderable(inventory);
        renderer.addRenderable(chat);
        this.position = position;
        Window.pushRenderable(renderer);
    }


    public void updateFrame() {
        particleCollector.unloadParticleEffects();
        particleCollector.uploadParticleEffects();
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
            camera.setPosition(toRenderPosition);
        }
    }

    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, camera.getRotation());
        }
        if (canDoActiveActions()) interactionHandler.updateGameTick();
        renderer.updateGameTick();
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
        if (button == KeySetting.ZOOM.value() && action != GLFW_REPEAT) camera.setZoomed(action == GLFW_PRESS);
        if (button == KeySetting.INVENTORY.value() && action == GLFW_PRESS) toggleInventory();
        if (button == KeySetting.OPEN_CHAT.value() && action == GLFW_PRESS) toggleChat();
        if (button == KeySetting.START_COMMAND.value() && action == GLFW_PRESS) startCommand();

        if (button == KeySetting.DEBUG_MENU.value() && action == GLFW_PRESS) renderer.toggleDebugScreen();
        if (button == KeySetting.RELOAD_MATERIALS.value() && action == GLFW_PRESS) Material.loadMaterials();
        if (button == KeySetting.NO_CLIP.value() && action == GLFW_PRESS) noClip = !noClip;
        if (button == KeySetting.GET_CHUNK_REBUILD_PLACEABLE.value() && action == GLFW_PRESS) hotbar.setContent(hotbar.getSelectedSlot(), new ChunkRebuildPlaceable());
        if (button == KeySetting.TOGGLE_CULLING_COMPUTATION.value() && action == GLFW_PRESS) renderer.toggleCullingCalculation();
    }

    public void handleInactiveScrollInput(double xScroll, double yScroll) {
        if (camera.isZoomed()) {
            final float zoomFactorChange = 0.9F;
            camera.changeZoom(yScroll > 0 ? zoomFactorChange : 1 / zoomFactorChange);
            return;
        }

        if (ToggleSetting.SCROLL_HOTBAR.value()) hotbar.setSelectedSlot(hotbar.getSelectedSlot() + (yScroll < 0.0 ? 1 : -1));
    }


    public Vector3i getMinCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Position(this.position).add(-hitboxSize.x * 0.5F, 0.0F, -hitboxSize.z * 0.5F).intPosition();
    }

    public Vector3i getMaxCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Position(this.position).add(hitboxSize.x * 0.5F, hitboxSize.y, hitboxSize.z * 0.5F).intPosition();
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

    public boolean isNoClip() {
        return noClip;
    }

    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    public boolean isChatOpen() {
        return chat.isVisible();
    }

    public void cleanUp() {
        meshCollector.cleanUp();
        particleCollector.cleanUp();
    }

    void startCommand() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        chat.setText("/");
        setInput();
    }

    void toggleChat() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        setInput();
    }

    void toggleInventory() {
        if (chat.isVisible()) return;
        inventory.setVisible(!inventory.isVisible());
        setInput();
    }

    private final MeshCollector meshCollector;
    private final ParticleCollector particleCollector;
    private final Camera camera;
    private final PlayerInput input;
    private final Movement movement;
    private final Renderer renderer;
    private final InteractionHandler interactionHandler;
    private final Hotbar hotbar;
    private final Inventory inventory;
    private final ChatTextField chat;

    private boolean noClip = false;
    private Position position; // Center of the players feet
}
