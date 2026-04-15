package game.menus;

import core.renderables.*;
import core.rendering_api.Window;
import core.language.CoreUiMessages;

import game.language.UiMessages;
import game.server.World;
import game.server.saving.WorldSaver;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.*;
import java.security.SecureRandom;

import static org.lwjgl.glfw.GLFW.*;

public final class WorldCreationMenu extends UiBackgroundElement {

    public WorldCreationMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        TextField nameField = new TextField(sizeToParent, new Vector2f(0.35F, 0.85F), UiMessages.WORLD_NAME);
        TextField seedField = new TextField(sizeToParent, new Vector2f(0.35F, 0.7F), UiMessages.WORLD_SEED);

        sizeToParent = new Vector2f(0.25F, 0.1F);
        UiButton backButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), getBackButtonClickable());
        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F), CoreUiMessages.BACK);
        backButton.addRenderable(text);

        UiButton createButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.7F), getCreateButtonClickable(nameField, seedField));
        text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.CREATE_WORLD);
        createButton.addRenderable(text);

        addRenderable(backButton);
        addRenderable(createButton);
        addRenderable(nameField);
        addRenderable(seedField);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new WorldCreationMenuInput(this));
    }

    private static Clickable getBackButtonClickable() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return false;
            Window.popRenderable();
            return true;
        };
    }

    private static Clickable getCreateButtonClickable(TextField nameField, TextField seedField) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS || nameField.getText().isEmpty()) return false;
            String worldName = sanitizeWorldName(nameField.getText());
            File[] savedWorlds = MainMenu.getSavedWorlds();
            for (File file : savedWorlds) if (file.getName().equals(worldName)) return false;

            long seed = getSeed(seedField.getText());
            new WorldSaver().save(new World(seed), WorldSaver.getSaveFileLocation(worldName));

            Window.popRenderable();
            return true;
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

    private static String sanitizeWorldName(String worldName) {
        char[] chars = worldName.toCharArray();
        for (int index = 0; index < chars.length; index++) if (!isAllowedChar(chars[index])) chars[index] = '_';
        return String.valueOf(chars);
    }

    private static boolean isAllowedChar(char character) {
        if (character >= '0' && character <= '9') return true;
        if (character >= 'a' && character <= 'z') return true;
        if (character >= 'A' && character <= 'Z') return true;
        return character == '_' || character == '-' || character == ' ';
    }
}
