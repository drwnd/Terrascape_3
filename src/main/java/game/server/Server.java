package game.server;

import core.assets.CoreSounds;
import core.rendering_api.CrashAction;
import core.rendering_api.CrashCallback;
import core.settings.CoreFloatSettings;
import core.settings.optionSettings.ColorOption;
import core.sound.Sound;
import core.utils.Vector3l;

import game.player.Player;
import game.player.interaction.Placeable;
import game.player.rendering.MeshCollector;
import game.server.command.Command;
import game.server.command.CommandResult;
import game.server.generation.ChunkGenerator;
import game.server.saving.ChunkSaver;
import game.settings.FloatSettings;
import game.settings.IntSettings;
import game.settings.ToggleSettings;
import game.utils.Position;
import game.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Server implements CrashCallback {

    public static final int TARGET_TPS = 20;
    public static final int NANOSECONDS_PER_SECOND = 1_000_000_000;

    /**
     * Constructs a new Server with specified initial state.
     * @param currentGameTick the current game tick
     * @param dayTime the current day-night cycle time
     * @param messages the list of chat messages
     */
    public Server(long currentGameTick, float dayTime, ArrayList<ChatMessage> messages) {
        this.currentGameTick = currentGameTick;
        this.dayTime = dayTime;
        this.messages = messages;
    }

    /**
     * Constructs a new Server based on an existing one.
     * @param oldServer the server instance to copy state from
     */
    public Server(Server oldServer) {
        oldServer.cleanUp();
        currentGameTick = oldServer.currentGameTick;
        dayTime = oldServer.dayTime;
        messages = oldServer.messages;
    }


    /**
     * Handles exceptions by cleaning up the game state.
     * @param exception the exception that occurred
     * @return the action to take after the crash (print and close)
     */
    @Override
    public CrashAction notify(Exception exception) {
        Game.cleanUp();
        return CrashAction.PRINT_AND_CLOSE;
    }

    public static void loadImmediateSurroundings() {
        ChunkGenerator.loadImmediateSurroundings();
    }

    /**
     * Unloads chunks that are far from the player to free memory.
     * @param playerChunkPosition the current player position in Chunk Coordinates (LOD 0)
     */
    public static void unloadDistantChunks(Vector3l playerChunkPosition) {
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        ChunkSaver saver = new ChunkSaver();

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            long lodPlayerX = playerChunkPosition.x >> lod;
            long lodPlayerY = playerChunkPosition.y >> lod;
            long lodPlayerZ = playerChunkPosition.z >> lod;

            for (Chunk chunk : Game.getWorld().getLod(lod)) {
                if (chunk == null) continue;

                if (Utils.outsideRenderKeepDistance(lodPlayerX, lodPlayerY, lodPlayerZ, chunk.X, chunk.Y, chunk.Z, chunk.LOD))
                    meshCollector.removeMesh(chunk.INDEX, chunk.LOD);

                if (Utils.outsideChunkKeepDistance(lodPlayerX, lodPlayerY, lodPlayerZ, chunk.X, chunk.Y, chunk.Z, chunk.LOD)) {
                    Game.getWorld().setNull(chunk.INDEX, chunk.LOD);
                    if (chunk.isModified()) saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
                }
            }
        }
    }

    /**
     * Unloads all currently loaded chunks from the world.
     */
    public static void unloadAll() {
        ChunkSaver saver = new ChunkSaver();

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++)
            for (Chunk chunk : Game.getWorld().getLod(lod)) {
                if (chunk == null) continue;
                Game.getWorld().setNull(chunk.INDEX, chunk.LOD);
                if (chunk.isModified()) saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
            }
    }


    /**
     * Calculates the fraction of the current game tick that has elapsed.
     * @return the elapsed fraction of the current game tick [0.0, 1.0]
     */
    public float getCurrentGameTickFraction() {
        long currentGameTickDuration = System.nanoTime() - gameTickStartTime;
        return (float) ((double) currentGameTickDuration / NANOSECONDS_PER_GAME_TICK);
    }

    public long getCurrentGameTick() {
        return currentGameTick;
    }

    public float getDayTime() {
        return dayTime;
    }

    public void setDayTime(float dayTime) {
        this.dayTime = dayTime;
    }

    /**
     * Processes a block breaking or placing interaction.
     * @param position the position where the interaction occurs (Absolute World Coordinates)
     * @param placeable the object being placed or the tool being used
     * @param side the side of the block being interacted with
     * @return true if the interaction was successful, false otherwise
     */
    public boolean requestBreakPlaceInteraction(Vector3l position, Placeable placeable, int side) {
        placeable.offsetPosition(position, side);

        Player player = Game.getPlayer();
        if (!ToggleSettings.NO_CLIP.value() && placeable.intersectsAABB(position, player.getMinCoordinate(), player.getMaxCoordinate())) return false;

        placeable.spawnParticles(position);

        MeshCollector meshCollector = player.getMeshCollector();
        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) placeable.place(position, lod);
        for (Chunk chunk : placeable.getAffectedChunks()) {
            if (chunk == null) continue;
            meshCollector.setMeshed(false, chunk.INDEX, chunk.LOD);
        }
        synchronized (generator) {
            generatorRestartScheduled = true;
        }
        return true;
    }

    /**
     * Stops the execution of game ticks.
     */
    public void pauseTicks() {
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

    /**
     * Starts or resumes the execution of game ticks at a fixed rate.
     */
    public void startTicks() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::executeGameTickCatchException, 0, NANOSECONDS_PER_GAME_TICK, TimeUnit.NANOSECONDS);
    }

    /**
     * Cleans up server resources and stops game ticks.
     */
    void cleanUp() {
        generator.cleanUp();
        pauseTicks();
    }

    /**
     * Schedules a restart of the chunk generator.
     */
    public void scheduleGeneratorRestart() {
        synchronized (generator) {
            generatorRestartScheduled = true;
        }
    }

    /**
     * Processes a message sent by the player, executing it as a command if it starts with '/'.
     * @param message the message text
     */
    public void sendPlayerMessage(String message) {
        if (message == null || message.isEmpty()) return;
        synchronized (messages) {
            messages.add(new ChatMessage(message, Sender.PLAYER, ColorOption.WHITE));
        }

        if (message.charAt(0) != '/') return;
        CommandResult result = Command.execute(message);

        if (!result.successful()) {
            sendServerMessage(result.reason(), ColorOption.RED);
            Sound.playUI(CoreSounds.BUTTON_FAILURE, CoreFloatSettings.UI_AUDIO);
        }
    }

    /**
     * Retrieves a copy of all current chat messages.
     * @return a list of chat messages
     */
    public ArrayList<ChatMessage> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Sends a message from the server to the chat.
     * @param message the message text
     * @param color the color to display the message in
     */
    public void sendServerMessage(String message, ColorOption color) {
        synchronized (messages) {
            messages.add(new ChatMessage(message, Sender.SERVER, color));
        }
    }

    /**
     * Trims the chat history to the specified maximum number of messages.
     * @param maxMessageCount the maximum number of messages to keep
     */
    public void removeOldChatMessages(int maxMessageCount) {
        synchronized (messages) {
            int toRemoveMessagesCount = messages.size() - maxMessageCount;
            if (toRemoveMessagesCount <= 0) return;
            for (int index = 0; index < maxMessageCount; index++) messages.set(index, messages.get(index + toRemoveMessagesCount));
            for (int removedMessageIndex = 0; removedMessageIndex < toRemoveMessagesCount; removedMessageIndex++) messages.removeLast();
        }
    }

    /**
     * Registers a function to be executed every game tick.
     * @param function the function to run
     * @param identifier an object used to identify and later remove the function
     */
    public void addFunction(Function function, Object identifier) {
        synchronized (functions) {
            functions.put(identifier, function);
        }
    }

    /**
     * Removes a registered function.
     * @param identifier the identifier of the function to remove
     * @return the removed function, or null if not found
     */
    public Function removeFunction(Object identifier) {
        synchronized (functions) {
            return functions.remove(identifier);
        }
    }


    /**
     * Executes a game tick and handles any exceptions that occur during its execution.
     */
    private void executeGameTickCatchException() {
        try {
            gameTickStartTime = System.nanoTime();
            executeGameTick();
            currentGameTick++;
            incrementTime();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Performs the logic for a single game tick, including player updates and scheduled functions.
     */
    private void executeGameTick() {
        Position oldPlayerPosition = Game.getPlayer().getPosition();
        Game.getPlayer().updateGameTick();
        synchronized (functions) {
            for (Iterator<Function> iterator = functions.values().iterator(); iterator.hasNext(); ) {
                boolean shouldRemove = !iterator.next().run();
                if (shouldRemove) iterator.remove();
            }
        }
        Position newPlayerPosition = Game.getPlayer().getPosition();
        synchronized (generator) {
            if ((!oldPlayerPosition.sharesChunkWith(newPlayerPosition) || generatorRestartScheduled) && ToggleSettings.CULLING_COMPUTATION.value()) {
                generator.restart();
                generatorRestartScheduled = false;
            }
        }
        removeOldChatMessages(IntSettings.MAX_CHAT_MESSAGE_COUNT.value());
    }

    /**
     * Advances the in-game time.
     */
    private void incrementTime() {
        dayTime += FloatSettings.TIME_SPEED.value();
        if (dayTime > 1.0F) dayTime -= 2.0F;
    }

    private long currentGameTick;
    private float dayTime;

    private ScheduledExecutorService executor = null;
    private final ArrayList<ChatMessage> messages;
    private final ChunkGenerator generator = new ChunkGenerator();
    private final HashMap<Object, Function> functions = new HashMap<>();

    private long gameTickStartTime;
    private boolean generatorRestartScheduled = true;

    private static final int NANOSECONDS_PER_GAME_TICK = NANOSECONDS_PER_SECOND / TARGET_TPS;
}
