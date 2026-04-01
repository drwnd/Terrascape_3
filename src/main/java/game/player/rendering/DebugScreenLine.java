package game.player.rendering;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.rendering_api.Window;
import core.rendering_api.shaders.TextShader;
import core.settings.CoreFloatSettings;
import core.settings.CoreOptionSettings;
import core.settings.OptionSetting;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.FontOption;
import core.settings.optionSettings.Visibility;
import core.utils.MathUtils;
import core.utils.StringGetter;
import core.utils.Vector3l;

import game.player.interaction.Target;
import game.server.Chunk;
import game.server.Game;
import game.server.generation.MapSample;
import game.server.generation.WorldGeneration;
import game.settings.DebugScreenOptions;
import game.utils.Position;
import game.utils.Status;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;

import static game.utils.Constants.*;

public record DebugScreenLine(OptionSetting visibility, OptionSetting color, StringGetter string, String name) {

    public boolean shouldShow(boolean debugScreenOpen) {
        return visibility.value() == Visibility.ALWAYS || debugScreenOpen && visibility.value() == Visibility.WHEN_SCREEN_OPEN;
    }

    public void render(int textLine) {
        Vector2f defaultTextSize = ((FontOption) CoreOptionSettings.FONT.value()).getDefaultTextSize();
        TextShader shader = (TextShader) AssetManager.get(CoreShaders.TEXT);
        shader.bind();

        float lineSeparation = defaultTextSize.y * CoreFloatSettings.TEXT_SIZE.value();
        Vector2f position = new Vector2f(0.0F, 1.0F - textLine * lineSeparation);
        Color color = ((ColorOption) this.color.value()).getColor();

        shader.drawText(position, string.get(), color, true, false);
    }

    public static ArrayList<DebugScreenLine> getDebugLines() {
        ArrayList<DebugScreenLine> lines = new ArrayList<>();

        add(lines, DebugScreenOptions.WORLD_NAME, () -> Game.getWorld().getName());

        add(lines, DebugScreenOptions.WORLD_TICK_AND_TIME, () -> "Current Tick:%s, Current Time:%s".formatted(Game.getServer().getCurrentGameTick(), Renderer.getRenderTime()));

        add(lines, DebugScreenOptions.FPS, () -> {
            ArrayList<Long> frameTimes = Game.getPlayer().getRenderer().getFrameTimes();
            long maxFrameTime = 0L, minFrameTime = Long.MAX_VALUE, frameTime = Window.getCPUFrameTime();
            for (int index = 0; index < frameTimes.size() - 1; index++) {
                maxFrameTime = Math.max(maxFrameTime, frameTimes.get(index + 1) - frameTimes.get(index));
                minFrameTime = Math.min(minFrameTime, frameTimes.get(index + 1) - frameTimes.get(index));
            }

            return "FPS: %s, lowest: %s, highest: %s, CPU Frame Time: %sµs"
                    .formatted(frameTimes.size(), (int) (1_000_000_000D / maxFrameTime), (int) (1_000_000_000D / minFrameTime), frameTime / 1_000L);
        });

        add(lines, DebugScreenOptions.TOTAL_MEMORY, () -> {
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = total - free;

            return "Total Memory: %sMB, used: %sMB, free: %sMB".formatted(total / 1_000_000L, used / 1_000_000L, free / 1_000_000L);
        });

        add(lines, DebugScreenOptions.CHUNK_MEMORY, () -> {
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
        });

        add(lines, DebugScreenOptions.BUFFER_STORAGE, () -> {
            MemoryAllocator allocator = Game.getPlayer().getMeshCollector().getAllocator();
            int used = allocator.getUsed() / 1000;
            int free = allocator.getFree() / 1000;
            int capacity = allocator.getCapacity() / 1000;
            int highestAllocated = allocator.getHighestAllocated() / 1000;

            return "Capacity:%sKB, Highest Allocated:%sKB, Used: %sKB, Free: %sKB".formatted(capacity, highestAllocated, used, free);
        });

        add(lines, DebugScreenOptions.RENDERED_MODELS, () -> {
            Renderer renderer = Game.getPlayer().getRenderer();
            return "Rendered Opaque Models:%s, Water Models:%s, Glass Models:%s".formatted(renderer.renderedOpaqueModels, renderer.renderedWaterModels, renderer.renderedGlassModels);
        });

        add(lines, DebugScreenOptions.POSITION, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            return "Position %s, Fraction %s".formatted(playerPosition.intPositionToString(), playerPosition.fractionToString());
        });

        add(lines, DebugScreenOptions.VELOCITY, () -> {
            Vector3f velocity = Game.getPlayer().getMovement().getVelocity();
            return "Velocity %sm/s : [X:%s, Y:%s, Z:%s]".formatted(MathUtils.round(velocity.length() * 20 / 16, 3), MathUtils.round(velocity.x, 3), MathUtils.round(velocity.y, 3), MathUtils.round(velocity.z, 3));
        });

        add(lines, DebugScreenOptions.CHUNK_POSITION, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            Chunk chunk = Game.getWorld().getChunk(
                    playerPosition.longX >>> CHUNK_SIZE_BITS,
                    playerPosition.longY >>> CHUNK_SIZE_BITS,
                    playerPosition.longZ >>> CHUNK_SIZE_BITS, 0);
            if (chunk == null) return "Chunk is null";
            return "Chunk Position [X:%s, Y:%s, Z:%s], In Chunk Position %s".formatted(chunk.X, chunk.Y, chunk.Z, playerPosition.inChunkPositionToString());
        });

        add(lines, DebugScreenOptions.DIRECTION, () -> {
            Vector3f direction = Game.getPlayer().getCamera().getDirection();
            return "Direction X:%s, Y:%s, Z:%s".formatted(direction.x, direction.y, direction.z);
        });

        add(lines, DebugScreenOptions.ROTATION, () -> {
            Vector3f rotation = Game.getPlayer().getCamera().getRotation();
            return "Rotation Pitch:%s, Yaw:%s, Roll:%s".formatted(rotation.x, rotation.y, rotation.z);
        });

        add(lines, DebugScreenOptions.TARGET, () -> {
            Target target = Target.getPlayerTarget();

            if (target == null) return "Nothing targeted.";
            return target.string();
        });

        add(lines, DebugScreenOptions.SEED, () -> "Seed: %s".formatted(WorldGeneration.SEED));

        add(lines, DebugScreenOptions.CHUNK_STATUS, () -> {
            Vector3l chunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
            Chunk chunk = Game.getWorld().getChunk(chunkCoordinate.x, chunkCoordinate.y, chunkCoordinate.z, 0);

            if (chunk == null) return "Chunk is null";
            return "Current Chunk generation status:%s, meshed:%s".formatted(chunk.getGenerationStatus().name(), Game.getPlayer().getMeshCollector().isMeshed(chunk.INDEX, 0));
        });

        add(lines, DebugScreenOptions.CHUNK_IDENTIFIERS, () -> {
            Vector3l chunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
            Chunk chunk = Game.getWorld().getChunk(chunkCoordinate.x, chunkCoordinate.y, chunkCoordinate.z, 0);

            if (chunk == null) return "Chunk is null";
            return "Chunk Index:%s, Chunk ID:%s".formatted(chunk.INDEX, chunk.ID);
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Temperature:        %s".formatted(MapSample.temperatureMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Humidity:           %s".formatted(MapSample.humidityMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Height:             %s".formatted(MapSample.heightMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Erosion:            %s".formatted(MapSample.erosionMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Continental:        %s".formatted(MapSample.continentalBaseMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Continental Addend: %s".formatted(MapSample.continentalAddendMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "River:              %s".formatted(MapSample.riverBaseMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.GENERATION_DATA, () -> {
            Position position = Game.getPlayer().getPosition();
            return "Ridge:              %s".formatted(MapSample.ridgeMapValue(position.longX, position.longZ));
        });

        add(lines, DebugScreenOptions.BIOME, () -> {
            Position position = Game.getPlayer().getPosition();
            MapSample sample = new MapSample(position.longX, position.longZ, true, true);
            int resultingHeight = WorldGeneration.getResultingHeight(sample);
            return "Biome: %s".formatted(WorldGeneration.getBiome(sample, resultingHeight, 0).getName());
        });

        return lines;
    }

    private static void add(ArrayList<DebugScreenLine> lines, DebugScreenOptions options, StringGetter string) {
        lines.add(new DebugScreenLine(options.getVisibility(), options.getColor(), string, options.name()));
    }
}

