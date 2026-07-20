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
 * Creates a new Server instance.
 *
 * @param currentGameTick parameter
 * @param dayTime parameter
 * @param messages parameter
 */
    public Server(long currentGameTick, float dayTime, ArrayList<ChatMessage> messages) {
        this.currentGameTick = currentGameTick;
        this.dayTime = dayTime;
        this.messages = messages;
    }

/**
 * Creates a new Server instance.
 *
 * @param oldServer parameter
 */
    public Server(Server oldServer) {
        oldServer.cleanUp();
        currentGameTick = oldServer.currentGameTick;
        dayTime = oldServer.dayTime;
        messages = oldServer.messages;
    }


/**
 * Performs notify.
 *
 * @param exception parameter
 * @return result
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
 * Performs unload distant chunks.
 *
 * @param playerChunkPosition parameter
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
 * Performs unload all.
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
 * Returns the current game tick fraction.
 * @return result
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
 * Performs request break place interaction.
 *
 * @param position parameter
 * @param placeable parameter
 * @param side parameter
 * @return true if the condition holds
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
 * Performs pause ticks.
 */
    public void pauseTicks() {
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

/**
 * Performs start ticks.
 */
    public void startTicks() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::executeGameTickCatchException, 0, NANOSECONDS_PER_GAME_TICK, TimeUnit.NANOSECONDS);
    }

/**
 * Performs clean up.
 */
    void cleanUp() {
        generator.cleanUp();
        pauseTicks();
    }

/**
 * Performs schedule generator restart.
 */
    public void scheduleGeneratorRestart() {
        synchronized (generator) {
            generatorRestartScheduled = true;
        }
    }

/**
 * Performs send player message.
 *
 * @param message parameter
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
 * Returns the messages.
 * @return result
 */
    public ArrayList<ChatMessage> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

/**
 * Performs send server message.
 *
 * @param message parameter
 * @param color parameter
 */
    public void sendServerMessage(String message, ColorOption color) {
        synchronized (messages) {
            messages.add(new ChatMessage(message, Sender.SERVER, color));
        }
    }

/**
 * Removes old chat messages.
 *
 * @param maxMessageCount parameter
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
 * Adds function.
 *
 * @param function parameter
 * @param identifier parameter
 */
    public void addFunction(Function function, Object identifier) {
        synchronized (functions) {
            functions.put(identifier, function);
        }
    }

/**
 * Removes function.
 *
 * @param identifier parameter
 * @return result
 */
    public Function removeFunction(Object identifier) {
        synchronized (functions) {
            return functions.remove(identifier);
        }
    }


/**
 * Performs execute game tick catch exception.
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
 * Performs execute game tick.
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
 * Performs increment time.
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
