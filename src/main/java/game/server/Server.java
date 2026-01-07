package game.server;

import core.rendering_api.CrashAction;
import core.rendering_api.CrashCallback;
import core.settings.FloatSetting;
import core.settings.optionSettings.ColorOption;

import game.player.Player;
import game.player.interaction.CubePlaceable;
import game.player.interaction.CuboidPlaceable;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.player.rendering.MeshCollector;
import game.server.command.Command;
import game.server.command.CommandResult;
import game.server.generation.ChunkGenerator;
import game.server.saving.ChunkSaver;
import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static game.utils.Constants.*;

public final class Server implements CrashCallback {

    public static final int TARGET_TPS = 20;
    public static final int NANOSECONDS_PER_SECOND = 1_000_000_000;

    public Server(long currentGameTick, float dayTime, ArrayList<ChatMessage> messages) {
        this.currentGameTick = currentGameTick;
        this.dayTime = dayTime;
        this.messages = messages;
    }


    @Override
    public CrashAction notify(Exception exception) {
        Game.cleanUp();
        return CrashAction.PRINT_AND_CLOSE;
    }

    public static void loadImmediateSurroundings() {
        ChunkGenerator.loadImmediateSurroundings();
    }

    public static void unloadDistantChunks(Vector3i playerChunkPosition) {
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        ChunkSaver saver = new ChunkSaver();

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int lodPlayerX = playerChunkPosition.x >> lod;
            int lodPlayerY = playerChunkPosition.y >> lod;
            int lodPlayerZ = playerChunkPosition.z >> lod;

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

    public static void unloadAll() {
        ChunkSaver saver = new ChunkSaver();

        for (int lod = 0; lod < LOD_COUNT; lod++)
            for (Chunk chunk : Game.getWorld().getLod(lod)) {
                if (chunk == null) continue;
                Game.getWorld().setNull(chunk.INDEX, chunk.LOD);
                if (chunk.isModified()) saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
            }
    }


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

    public boolean requestBreakPlaceInteraction(Vector3i position, Placeable placeable) {
        placeable.offsetPosition(position);

        Player player = Game.getPlayer();
        if (!player.isNoClip() && placeable.intersectsAABB(position, player.getMinCoordinate(), player.getMaxCoordinate())) return false;

        if (placeable instanceof CubePlaceable cubePlaceable) {
            int breakPlaceSize = 1 << Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
            player.getParticleCollector().addBreakParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, cubePlaceable.getMaterial());
            player.getParticleCollector().addPlaceParticleEffect(position.x, position.y, position.z, breakPlaceSize, breakPlaceSize, breakPlaceSize, cubePlaceable.getMaterial());
        }
        if (placeable instanceof CuboidPlaceable cuboidPlaceable) {
            Vector3i minPosition = cuboidPlaceable.getMinPosition();
            Vector3i length = new Vector3i(cuboidPlaceable.getMaxPosition()).sub(minPosition).add(1, 1, 1);
            player.getParticleCollector().addBreakParticleEffect(minPosition.x, minPosition.y, minPosition.z, length.x, length.y, length.z, cuboidPlaceable.getMaterial());
            player.getParticleCollector().addPlaceParticleEffect(minPosition.x, minPosition.y, minPosition.z, length.x, length.y, length.z, cuboidPlaceable.getMaterial());
        }
        if (placeable instanceof StructurePlaceable structurePlaceable) {
            player.getParticleCollector().addPlaceParticleEffect(position.x, position.y, position.z, structurePlaceable.getStructure());
        }

        MeshCollector meshCollector = player.getMeshCollector();
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            placeable.place(position, lod);
            for (Chunk chunk : placeable.getAffectedChunks()) {
                if (chunk == null) continue;
                meshCollector.setMeshed(false, chunk.INDEX, chunk.LOD);
            }
        }
        synchronized (generator) {
            generatorRestartScheduled = true;
        }
        return true;
    }

    public void pauseTicks() {
        if (executor != null) executor.shutdownNow();
    }

    public void startTicks() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::executeGameTickCatchException, 0, NANOSECONDS_PER_GAME_TICK, TimeUnit.NANOSECONDS);
    }

    void cleanUp() {
        generator.cleanUp();
        pauseTicks();
    }

    public void scheduleGeneratorRestart() {
        synchronized (generator) {
            generatorRestartScheduled = true;
        }
    }

    public void sendPlayerMessage(String message) {
        if (message == null || message.isEmpty()) return;
        synchronized (messages) {
            messages.add(new ChatMessage(message, Sender.PLAYER, ColorOption.WHITE));
        }

        if (message.charAt(0) != '/') return;
        CommandResult result = Command.execute(message);

        if (!result.successful()) sendServerMessage(result.reason(), ColorOption.RED);
    }

    public ArrayList<ChatMessage> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    public void sendServerMessage(String message, ColorOption color) {
        synchronized (messages) {
            messages.add(new ChatMessage(message, Sender.SERVER, color));
        }
    }

    public void removeOldChatMessages(int maxMessageCount) {
        synchronized (messages) {
            int toRemoveMessagesCount = messages.size() - maxMessageCount;
            if (toRemoveMessagesCount <= 0) return;
            for (int index = 0; index < maxMessageCount; index++) messages.set(index, messages.get(index + toRemoveMessagesCount));
            for (int removedMessageIndex = 0; removedMessageIndex < toRemoveMessagesCount; removedMessageIndex++) messages.removeLast();
        }
    }

    public void addFunction(Function function, Object identifier) {
        synchronized (functions) {
            functions.put(identifier, function);
        }
    }

    public Function removeFunction(Object identifier) {
        synchronized (functions) {
            return functions.remove(identifier);
        }
    }


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
            if ((!oldPlayerPosition.sharesChunkWith(newPlayerPosition) || generatorRestartScheduled) && Game.getPlayer().getRenderer().isCalculateCulling()) {
                generator.restart();
                generatorRestartScheduled = false;
            }
        }
        removeOldChatMessages((int) FloatSetting.MAX_CHAT_MESSAGE_COUNT.value());
    }

    private void incrementTime() {
        dayTime += FloatSetting.TIME_SPEED.value();
        if (dayTime > 1.0F) dayTime -= 2.0F;
    }

    private long currentGameTick;
    private float dayTime;

    private ScheduledExecutorService executor;
    private final ArrayList<ChatMessage> messages;
    private final ChunkGenerator generator = new ChunkGenerator();
    private final HashMap<Object, Function> functions = new HashMap<>();

    private long gameTickStartTime;
    private boolean generatorRestartScheduled = true;

    private static final int NANOSECONDS_PER_GAME_TICK = NANOSECONDS_PER_SECOND / TARGET_TPS;
}
