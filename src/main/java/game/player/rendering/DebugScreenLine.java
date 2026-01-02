package game.player.rendering;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.rendering_api.Window;
import core.settings.OptionSetting;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.FontOption;
import core.settings.optionSettings.Visibility;
import core.utils.StringGetter;

import game.player.interaction.Target;
import game.server.Chunk;
import game.server.Game;
import game.server.generation.WorldGeneration;
import game.utils.Position;

import game.utils.Status;
import game.utils.Utils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.*;
import java.util.ArrayList;

import static game.utils.Constants.*;

public record DebugScreenLine(OptionSetting visibility, OptionSetting color, StringGetter string, String name) {

    public boolean shouldShow(boolean debugScreenOpen) {
        return visibility.value() == Visibility.ALWAYS || debugScreenOpen && visibility.value() == Visibility.WHEN_SCREEN_OPEN;
    }

    public void render(int textLine) {
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        TextShader shader = (TextShader) AssetManager.get(CoreShaders.TEXT);
        shader.bind();

        float lineSeparation = defaultTextSize.y * FloatSetting.TEXT_SIZE.value();
        Vector2f position = new Vector2f(0.0F, 1.0F - textLine * lineSeparation);
        Color color = ((ColorOption) this.color.value()).getColor();

        shader.drawText(position, string.get(), color, true, false);
    }

    public static ArrayList<DebugScreenLine> getDebugLines() {
        ArrayList<DebugScreenLine> lines = new ArrayList<>();

        lines.add(new DebugScreenLine(OptionSetting.WORLD_NAME_VISIBILITY, OptionSetting.WORLD_NAME_COLOR,
                () -> Game.getWorld().getName(), "World name"));

        lines.add(new DebugScreenLine(OptionSetting.WORLD_TICK_AND_TIME_VISIBILITY, OptionSetting.WORLD_TICK_AND_TIME_COLOR,
                () -> "Current Tick:%s, Current Time:%s".formatted(Game.getServer().getCurrentGameTick(), Renderer.getRenderTime()),
                "Gametick and time"));

        lines.add(new DebugScreenLine(OptionSetting.FPS_VISIBILITY, OptionSetting.FPS_COLOR, () -> {
            ArrayList<Long> frameTimes = Game.getPlayer().getRenderer().getFrameTimes();
            long maxFrameTime = 0L, minFrameTime = Long.MAX_VALUE, frameTime = Window.getCPUFrameTime();
            for (int index = 0; index < frameTimes.size() - 1; index++) {
                maxFrameTime = Math.max(maxFrameTime, frameTimes.get(index + 1) - frameTimes.get(index));
                minFrameTime = Math.min(minFrameTime, frameTimes.get(index + 1) - frameTimes.get(index));
            }

            return "FPS: %s, lowest: %s, highest: %s, CPU Frame Time: %sµs"
                    .formatted(frameTimes.size(), (int) (1_000_000_000D / maxFrameTime), (int) (1_000_000_000D / minFrameTime), frameTime / 1_000L);
        }, "FPS"));

        lines.add(new DebugScreenLine(OptionSetting.TOTAL_MEMORY_VISIBILITY, OptionSetting.TOTAL_MEMORY_COLOR, () -> {
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = total - free;

            return "Total Memory: %sMB, used: %sMB, free: %sMB".formatted(total / 1_000_000L, used / 1_000_000L, free / 1_000_000L);
        }, "Total Memory"));

        lines.add(new DebugScreenLine(OptionSetting.CHUNK_MEMORY_VISIBILITY, OptionSetting.CHUNK_MEMORY_COLOR, () -> {
            long memory = 0L;
            int chunks = 0;
            for (int lod = 0; lod < LOD_COUNT; lod++)
                for (Chunk chunk : Game.getWorld().getLod(lod)) {
                    if (chunk == null || chunk.getGenerationStatus() != Status.DONE) continue;
                    memory += chunk.getMaterials().getBytes().length;
                    chunks++;
                }
            if (chunks == 0) return "No generated Chunks";
            return "Chunk Memory: %sMB, average: %sB".formatted(memory / 1_000_000L, memory / chunks);
        }, "Chunk Memory"));

        lines.add(new DebugScreenLine(OptionSetting.BUFFER_STORAGE_VISIBILITY, OptionSetting.BUFFER_STORAGE_COLOR, () -> {
            MemoryAllocator allocator = Game.getPlayer().getMeshCollector().getAllocator();
            int used = allocator.getUsed() / 1000;
            int free = allocator.getFree() / 1000;
            int capacity = allocator.getCapacity() / 1000;
            int highestAllocated = allocator.getHighestAllocated() / 1000;

            return "Capacity:%sKB, Highest Allocated:%sKB, Used: %sKB, Free: %sKB".formatted(capacity, highestAllocated, used, free);
        }, "Mesh Buffer Storage Info"));

        lines.add(new DebugScreenLine(OptionSetting.RENDERED_MODELS_VISIBILITY, OptionSetting.RENDERED_MODELS_COLOR, () -> {
            Renderer renderer = Game.getPlayer().getRenderer();
            return "Rendered Opaque Models:%s, Water Models:%s, Glass Models:%s".formatted(renderer.renderedOpaqueModels, renderer.renderedWaterModels, renderer.renderedGlassModels);
        }, "Rendered Models Count"));

        lines.add(new DebugScreenLine(OptionSetting.POSITION_VISIBILITY, OptionSetting.POSITION_COLOR, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            return "Position %s, Fraction %s".formatted(playerPosition.intPositionToString(), playerPosition.fractionToString());
        }, "Player Position"));

        lines.add(new DebugScreenLine(OptionSetting.VELOCITY_VISIBILITY, OptionSetting.VELOCITY_COLOR, () -> {
            Vector3f velocity = Game.getPlayer().getMovement().getVelocity();
            return "Velocity %sm/s : [X:%s, Y:%s, Z:%s]".formatted(Utils.round(velocity.length() * 20 / 16, 3), Utils.round(velocity.x, 3), Utils.round(velocity.y, 3), Utils.round(velocity.z, 3));
        }, "Player Velocity"));

        lines.add(new DebugScreenLine(OptionSetting.CHUNK_POSITION_VISIBILITY, OptionSetting.CHUNK_POSITION_COLOR, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            Chunk chunk = Game.getWorld().getChunk(
                    playerPosition.intX >> CHUNK_SIZE_BITS,
                    playerPosition.intY >> CHUNK_SIZE_BITS,
                    playerPosition.intZ >> CHUNK_SIZE_BITS, 0);
            if (chunk == null) return "Chunk is null";
            return "Chunk Position [X:%s, Y:%s, Z:%s], In Chunk Position %s".formatted(chunk.X, chunk.Y, chunk.Z, playerPosition.inChunkPositionToString());
        }, "Chunk Position"));

        lines.add(new DebugScreenLine(OptionSetting.DIRECTION_VISIBILITY, OptionSetting.DIRECTION_COLOR, () -> {
            Vector3f direction = Game.getPlayer().getCamera().getDirection();
            return "Direction X:%s, Y:%s, Z:%s".formatted(direction.x, direction.y, direction.z);
        }, "Player Direction"));

        lines.add(new DebugScreenLine(OptionSetting.ROTATION_VISIBILITY, OptionSetting.ROTATION_COLOR, () -> {
            Vector3f rotation = Game.getPlayer().getCamera().getRotation();
            return "Rotation Pitch:%s, Yaw:%s, Roll:%s".formatted(rotation.x, rotation.y, rotation.z);
        }, "Player Rotation"));

        lines.add(new DebugScreenLine(OptionSetting.TARGET_VISIBILITY, OptionSetting.TARGET_COLOR, () -> {
            Target target = Target.getPlayerTarget();

            if (target == null) return "Nothing targeted.";
            return target.string();
        }, "Target Information"));

        lines.add(new DebugScreenLine(OptionSetting.SEED_VISIBILITY, OptionSetting.SEED_COLOR, () -> "Seed: %s".formatted(WorldGeneration.SEED),
                "Seed"));

        lines.add(new DebugScreenLine(OptionSetting.CHUNK_STATUS_VISIBILITY, OptionSetting.CHUNK_STATUS_COLOR, () -> {
            Vector3i chunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
            Chunk chunk = Game.getWorld().getChunk(chunkCoordinate.x, chunkCoordinate.y, chunkCoordinate.z, 0);

            if (chunk == null) return "Chunk is null";
            return "Current Chunk generation status:%s, meshed:%s".formatted(chunk.getGenerationStatus().name(), Game.getPlayer().getMeshCollector().isMeshed(chunk.INDEX, 0));
        }, "Chunk status"));

        lines.add(new DebugScreenLine(OptionSetting.CHUNK_IDENTIFIERS_VISIBILITY, OptionSetting.CHUNK_IDENTIFIERS_COLOR, () -> {
            Vector3i chunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
            Chunk chunk = Game.getWorld().getChunk(chunkCoordinate.x, chunkCoordinate.y, chunkCoordinate.z, 0);

            if (chunk == null) return "Chunk is null";
            return "Chunk Index:%s, Chunk ID:%s".formatted(chunk.INDEX, chunk.ID);
        }, "Chunk Identifiers"));

        return lines;
    }
}

