package game.player.rendering;

import core.assets.AssetManager;
import game.assets.Shaders;
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

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.*;
import java.util.ArrayList;

public record DebugScreenLine(OptionSetting visibility, OptionSetting color, StringGetter string, String name) {

    public boolean shouldShow(boolean debugScreenOpen) {
        return visibility.value() == Visibility.ALWAYS || debugScreenOpen && visibility.value() == Visibility.WHEN_SCREEN_OPEN;
    }

    public void render(int textLine) {
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        TextShader shader = (TextShader) AssetManager.getShader(Shaders.TEXT);
        shader.bind();

        float lineSeparation = defaultTextSize.y * FloatSetting.TEXT_SIZE.value();
        Vector2f position = new Vector2f(0.0f, 1.0f - textLine * lineSeparation);
        Color color = ((ColorOption) this.color.value()).getColor();

        shader.drawText(position, string.get(), color, true, false);
    }

    public static ArrayList<DebugScreenLine> getDebugLines() {
        ArrayList<DebugScreenLine> lines = new ArrayList<>();

        lines.add(new DebugScreenLine(OptionSetting.WORLD_NAME_VISIBILITY, OptionSetting.WORLD_NAME_COLOR,
                () -> Game.getWorld().getName(), "World name"));

        lines.add(new DebugScreenLine(OptionSetting.WORLD_TICK_AND_TIME_VISIBILITY, OptionSetting.WORLD_TICK_AND_TIME_COLOR,
                () -> "Current Tick:%s, Current Time:%s".formatted(Game.getServer().getCurrentGameTick(), Game.getPlayer().getRenderer().getTime()),
                "Gametick and time"));

        lines.add(new DebugScreenLine(OptionSetting.FPS_VISIBILITY, OptionSetting.FPS_COLOR, () -> {
            ArrayList<Long> frameTimes = Game.getPlayer().getRenderer().getFrameTimes();
            return "FPS: %s".formatted(frameTimes.size());
        }, "FPS"));

        lines.add(new DebugScreenLine(OptionSetting.RENDERED_MODELS_VISIBILITY, OptionSetting.RENDERED_MODELS_COLOR, () -> {
            Renderer renderer = Game.getPlayer().getRenderer();
            return "Rendered Opaque Models:%s, Transparent Models:%s".formatted(renderer.renderedOpaqueModels, renderer.renderedTransparentModels);
        }, "Rendered Models Count"));

        lines.add(new DebugScreenLine(OptionSetting.POSITION_VISIBILITY, OptionSetting.POSITION_COLOR, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            return "Position %s, Fraction %s".formatted(playerPosition.intPositionToString(), playerPosition.fractionToString());
        }, "Player Position"));

        lines.add(new DebugScreenLine(OptionSetting.CHUNK_POSITION_VISIBILITY, OptionSetting.CHUNK_POSITION_COLOR, () -> {
            Position playerPosition = Game.getPlayer().getPosition();
            return "Chunk Position %s, In Chunk Position %s".formatted(playerPosition.chunkCoordinateToString(), playerPosition.inChunkPositionToString());
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
            return "Current Chunk generated:%s, meshed:%s".formatted(chunk.isGenerated(), Game.getPlayer().getMeshCollector().isMeshed(chunk.INDEX, 0));
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

