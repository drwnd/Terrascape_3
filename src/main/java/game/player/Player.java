package game.player;

import core.rendering_api.Input;
import core.rendering_api.Window;
import core.sound.Sound;
import core.utils.Vector3l;

import game.assets.Model;
import game.assets.Models;
import game.player.interaction.*;
import game.player.inventory.Inventory;
import game.player.movement.Movement;
import game.player.rendering.Camera;
import game.player.rendering.MeshCollector;
import game.player.particles.ParticleCollector;
import game.player.rendering.Renderer;
import game.server.Game;
import game.settings.IntSettings;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import core.utils.MainThread;
import game.utils.Position;

import game.utils.ServerThread;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Player {

    @MainThread
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

    @MainThread
    public Position updateFrame() {
        Sound.setListenerData(camera.getPosition(), camera.getDirection(), movement.getVelocity());
        particleCollector.unloadParticleEffects();
        particleCollector.uploadParticleEffects();
        particleCollector.clearToBufferParticleEffects();
        meshCollector.uploadAllMeshes();
        meshCollector.deleteOldMeshes();

        Position toRenderPosition;
        float fraction = Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0.0F, 1.0F);
        synchronized (this) {
            camera.rotate(input.getCursorMovement());
            Vector3f movementThisTick = movement.getRenderVelocity().mul(fraction - 1.0F);
            toRenderPosition = new Position(position)
                    .add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
            camera.setPosition(camera.applyPerspectiveOffset(new Position(toRenderPosition).addComponent(Y_COMPONENT, movement.getState().getCameraElevation())));
        }
        return toRenderPosition;
    }

    @ServerThread
    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, camera.getRotation());
        }
        if (canDoActiveActions()) interactionHandler.updateGameTick();
        renderer.updateGameTick();
        camera.updateGameTick();

        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (canDoActiveActions() && currentGameTick >= lastInteractionTick + IntSettings.BREAK_PLACE_INTERVALL.value()
                && (Input.isKeyPressed(KeySettings.DESTROY) || Input.isKeyPressed(KeySettings.USE)))
            lastInteractionTick = currentGameTick;
    }

    @MainThread
    public void updateRenderDistance(int oldRenderDistance) {
        meshCollector = new MeshCollector(meshCollector, oldRenderDistance);
        renderer.reloadRenderingOptimizer();
    }

    @MainThread
    public void updateLodCount() {
        meshCollector = new MeshCollector(meshCollector);
        renderer.reloadRenderingOptimizer();
    }

    /**
     * Intended for actions that should not be taken when a menu is displayed.
     * For example, movement, block interactions, etc.
     */
    @MainThread
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

        if (action == GLFW_PRESS && (button == KeySettings.DESTROY.keybind() || button == KeySettings.USE.keybind()))
            lastInteractionTick = Game.getServer().getCurrentGameTick();
        if (button == KeySettings.ZOOM.keybind() && action != GLFW_REPEAT) camera.setZoomed(action == GLFW_PRESS);
    }

    /**
     * Intended for actions that could always be taken.
     * For example, Closing a menu or toggling the debug screen.
     */
    @MainThread
    public void handleInactiveKeyInput(int button, int action) {
        InteractionHandler.handleInactiveInput(button, action);

        if (button == KeySettings.OPEN_INVENTORY.keybind() && action == GLFW_PRESS) toggleInventory();
        if (button == KeySettings.OPEN_CHAT.keybind() && action == GLFW_PRESS) toggleChat();
        if (button == KeySettings.START_COMMAND.keybind() && action == GLFW_PRESS) startCommand();
        if (button == KeySettings.TAKE_SCREENSHOT.keybind() && action == GLFW_PRESS) Renderer.takeScreenshot();
    }

    @MainThread
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

    @MainThread
    public void applyAnimation(Model playerCharacter) {
        final int animationLength = 8;
        long currentTick = Game.getServer().getCurrentGameTick();
        if (currentTick > lastInteractionTick + animationLength) return;

        float ticksPassed = Math.min(Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0, 1) + (currentTick - lastInteractionTick), animationLength);

        playerCharacter.transforms()[Models.RIGHT_ARM]
                .rotate((float) Math.sin(ticksPassed * Math.PI / animationLength), 0, 0, 1)
                .rotate((float) Math.sin(ticksPassed * Math.PI * 2 / animationLength) * (1 - ticksPassed / animationLength) * 2, 1, 0, 0);
    }


    public Vector3l getMinCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Vector3l(Movement.minX(position, hitboxSize), Movement.minY(position, hitboxSize), Movement.minZ(position, hitboxSize));
    }

    public Vector3l getMaxCoordinate() {
        Vector3i hitboxSize = movement.getState().getHitboxSize();
        return new Vector3l(Movement.maxX(position, hitboxSize), Movement.maxY(position, hitboxSize), Movement.maxZ(position, hitboxSize));
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

    @MainThread
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

    @MainThread
    public boolean isChatOpen() {
        return chat.isVisible();
    }

    @MainThread
    public void cleanUp() {
        meshCollector.cleanUp();
        particleCollector.cleanUp();
    }

    @MainThread
    void startCommand() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        chat.setText("/");
        setInput();
    }

    @MainThread
    void toggleChat() {
        if (inventory.isVisible()) return;
        chat.setVisible(!chat.isVisible());
        setInput();
    }

    @MainThread
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

    private Position position; // Center of the players' feet
    private long lastInteractionTick = 0;
}
