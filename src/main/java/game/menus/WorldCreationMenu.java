package game.menus;

import core.renderables.*;
import core.rendering_api.MenuInput;
import core.rendering_api.Window;
import core.language.CoreUiMessages;
import core.settings.OptionSetting;
import core.settings.stand_alones.StandAloneOptionSetting;

import game.language.UiMessages;
import game.server.World;
import game.server.generation.BiomeSamplers;
import game.server.generation.BlockSizes;
import game.server.generation.WorldGenerationSettings;
import game.server.saving.WorldSaver;
import core.utils.MainThread;

import game.utils.Utils;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.*;
import java.security.SecureRandom;
import java.util.Date;

import static org.lwjgl.glfw.GLFW.*;

public final class WorldCreationMenu extends UiBackgroundElement {

    @MainThread
    public WorldCreationMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        TextField nameField = new TextField(sizeToParent, new Vector2f(0.35F, 0.85F), UiMessages.WORLD_NAME);
        TextField seedField = new TextField(sizeToParent, new Vector2f(0.35F, 0.7F), UiMessages.WORLD_SEED);

        sizeToParent = new Vector2f(0.25F, 0.1F);
        UiButton backButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F), CoreUiMessages.BACK);
        backButton.addRenderable(text);

        OptionSetting biomeSetting = new StandAloneOptionSetting(BiomeSamplers.DEFAULT);
        OptionSetting blockSizeSetting = new StandAloneOptionSetting(BlockSizes.LOD_0);
        UiButton biomeSamplerButton = new OptionToggle(sizeToParent, new Vector2f(0.35F, 0.55F), biomeSetting, UiMessages.BIOME_OPTION, true);
        UiButton blockSizeButton = new OptionToggle(sizeToParent, new Vector2f(0.65F, 0.55F), blockSizeSetting, UiMessages.BLOCK_SIZE_OPTION, true);

        UiButton createButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.7F), getCreateButtonClickable(nameField, seedField, biomeSetting, blockSizeSetting));
        text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.CREATE_WORLD);
        createButton.addRenderable(text);

        addRenderable(backButton);
        addRenderable(createButton);
        addRenderable(nameField);
        addRenderable(seedField);
        addRenderable(biomeSamplerButton);
        addRenderable(blockSizeButton);
    }

    @MainThread
    @Override
    public void setOnTop() {
        Window.setInput(new MenuInput<>(this));
    }


    @MainThread
    private static Clickable getCreateButtonClickable(TextField nameField, TextField seedField, OptionSetting biomeSetting, OptionSetting blockSizeSetting) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            if (nameField.getText().isEmpty()) return ButtonResult.FAILURE;
            String worldName = Utils.sanitizeFileName(nameField.getText());
            File[] savedWorlds = MainMenu.getSavedWorlds();
            for (File file : savedWorlds) if (file.getName().equalsIgnoreCase(worldName)) return ButtonResult.FAILURE;

            long seed = getSeed(seedField.getText());
            new WorldSaver().save(new World(
                    new WorldGenerationSettings(seed, (BiomeSamplers) biomeSetting.value(), (1 << blockSizeSetting.value().ordinal())),
                    new Date(), new Date(0), false), WorldSaver.getSaveFileLocation(worldName));

            Window.popRenderable();
            return ButtonResult.SUCCESS;
        };
    }

    private static long getSeed(String seedString) {
        if (seedString.isEmpty()) return getRandomSeed();

        try {
            return Long.parseLong(seedString);
        } catch (NumberFormatException ignore) {

        }
        long[] longs = toLongArray(seedString.toCharArray());
        long seed = 0;
        for (long aLong : longs) seed ^= aLong;
        return seed;
    }

    private static long getRandomSeed() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);    // Complete overkill but funny
        return (bytes[0] & 0xFFL) << 56 | (bytes[1] & 0xFFL) << 48 | (bytes[2] & 0xFFL) << 40 | (bytes[3] & 0xFFL) << 32
                | (bytes[4] & 0xFFL) << 24 | (bytes[5] & 0xFFL) << 16 | (bytes[6] & 0xFFL) << 8 | (bytes[7] & 0xFFL);
    }

    private static long[] toLongArray(char[] charArray) {
        long[] longs = new long[charArray.length / 8 + 1];
        for (int index = 0; index < longs.length; index++) {
            long current = 0;

            for (int charIndex = index * 8; charIndex < charArray.length && charIndex < (index + 1) * 8; charIndex++) {
                current <<= 8;
                current |= charArray[charIndex] & 0xFFL;
            }

            longs[index] = current;
        }
        return longs;
    }
}
