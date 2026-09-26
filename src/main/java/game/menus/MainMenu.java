package game.menus;

import core.renderables.*;
import core.rendering_api.MenuInput;
import core.utils.FileManager;
import core.language.Language;
import core.language.CoreUiMessages;
import core.rendering_api.Window;

import core.utils.Message;
import game.language.UiMessages;
import game.server.Game;
import game.server.World;
import game.server.WorldOptimizer;

import game.server.saving.WorldSaver;
import core.utils.MainThread;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public final class MainMenu extends UiBackgroundElement {

    @MainThread
    public MainMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        Vector2f sizeToParent = new Vector2f(0.25F, 0.1F);

        UiButton closeApplicationButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.QUIT_GAME);
        closeApplicationButton.addRenderable(text);

        UiButton createNewWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.725F), () -> Window.pushRenderable(new WorldCreationMenu()));
        text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.NEW_WORLD);
        createNewWorldButton.addRenderable(text);

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.6F), getSettingsAction());
        settingsButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), CoreUiMessages.SETTINGS));

        playWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.475F));
        playWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F)));

        deleteWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.05F));
        deleteWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F)));

        optimizeWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.35F));
        optimizeWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 2.0F / 3.0F)));
        optimizeWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 1.0F / 3.0F), UiMessages.WORLD_OPTIMIZER_TIME_WARNING));

        confirmDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.175F));
        confirmDeletionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.CONFIRM_DELETE_WORLD, Color.RED));

        cancelDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.05F));
        cancelDeletionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.KEEP_WORLD, Color.GREEN));
        cancelDeletionButton.setAction(this::hideWorldSpecificButtons);

        hideWorldSpecificButtons();

        addRenderable(settingsButton);
        addRenderable(createNewWorldButton);
        addRenderable(closeApplicationButton);
        addRenderable(playWorldButton);
        addRenderable(deleteWorldButton);
        addRenderable(optimizeWorldButton);
        addRenderable(confirmDeletionButton);
        addRenderable(cancelDeletionButton);
    }

    @MainThread
    public void moveWorldButtons(float movement) {
        Vector2f offset = new Vector2f(0, movement);
        for (Renderable renderable : worldButtons) renderable.move(offset);
    }


    @MainThread
    @Override
    public void setOnTop() {
        // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
        input = new MenuInput<>(this, this::moveWorldButtons, this::getMaxScroll);
        Window.setInput(input);
        createWorldButtons();
        hideWorldSpecificButtons();
    }

    @MainThread
    @Override
    public boolean clickOn(Vector2i pixelCoordinate, int mouseButton, int action) {
        boolean buttonFound = false;
        for (Renderable button : getChildren())
            if (button.isVisible() && button.containsPixelCoordinate(pixelCoordinate)) {
                button.clickOn(pixelCoordinate, mouseButton, action);
                buttonFound = true;
                break;
            }

        if (!buttonFound) hideWorldSpecificButtons();
        return true;
    }

    @MainThread
    private void setSelectedWorld(World world) {
        hideWorldSpecificButtons();

        playWorldButton.setAction(() -> Game.play(world));
        deleteWorldButton.setAction(getDeleteWorldAction(world));
        optimizeWorldButton.setAction(getOptimizeWorldAction(world));

        playWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.PLAY_WORLD).formatted(world.name));
        deleteWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.DELETE_WORLD).formatted(world.name));
        optimizeWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.OPTIMIZE_WORLD).formatted(world.name));

        playWorldButton.setVisible(true);
        deleteWorldButton.setVisible(true);
        optimizeWorldButton.setVisible(true);
    }

    @MainThread
    private void hideWorldSpecificButtons() {
        playWorldButton.setVisible(false);
        deleteWorldButton.setVisible(false);
        optimizeWorldButton.setVisible(false);
        confirmDeletionButton.setVisible(false);
        cancelDeletionButton.setVisible(false);
    }

    @MainThread
    public static File[] getSavedWorlds() {
        return FileManager.getChildren(Path.of("saves"));
    }

    @MainThread
    private void createWorldButtons() {
        for (Renderable worldButton : worldButtons) removeRenderable(worldButton).delete();
        worldButtons.clear();

        WorldSaver saver = new WorldSaver();
        File[] savedWorlds = getSavedWorlds();
        World[] worlds = new World[savedWorlds.length];

        for (int index = 0; index < savedWorlds.length; index++)
            worlds[index] = saver.load(WorldSaver.getSaveFileLocation(savedWorlds[index].getName()));

        Arrays.sort(worlds, (World a, World b) -> Long.signum(b.lastPlayedAsMs - a.lastPlayedAsMs));

        for (int index = 0; index < worlds.length; index++) {
            UiButton button = getPlayWorldButton(index, worlds[index]);

            addRenderable(button);
            worldButtons.add(button);
        }
    }

    @MainThread
    private UiButton getPlayWorldButton(int index, World world) {
        Vector2f sizeToParent = new Vector2f(0.6F, 0.125F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.035F - 0.14F * (index + 1) + input.getScroll());

        UiButton button = new UiButton(sizeToParent, offsetToParent, () -> setSelectedWorld(world));

        String worldInfo = UiMessages.WORLD_INFO_TEMPLATE.get().formatted(world.created, world.lastPlayed);
        TextElement worldInfoText = new TextElement(new Vector2f(0.05F, 1 / 3F), new Message(worldInfo), Color.LIGHT_GRAY);
        TextElement worldNameText = new TextElement(new Vector2f(0.05F, 2 / 3F), new Message(world.name));
        worldNameText.setTextSize(1.5F);

        button.addRenderable(worldNameText);
        button.addRenderable(worldInfoText);

        return button;
    }

    @MainThread
    private Clickable getDeleteWorldAction(World world) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            hideWorldSpecificButtons();

            confirmDeletionButton.setAction(getConfirmDeletionAction(world));
            confirmDeletionButton.setVisible(true);
            cancelDeletionButton.setVisible(true);
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    private Clickable getConfirmDeletionAction(World world) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            FileManager.delete(WorldSaver.getSaveFileLocation(world.fileName).getParent().toFile());
            createWorldButtons();
            hideWorldSpecificButtons();
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    private Clickable getOptimizeWorldAction(World world) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            WorldOptimizer.optimize(world);
            hideWorldSpecificButtons();
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    private float getMaxScroll() {
        return worldButtons.size() * 0.14F - 1 + 0.0825F;
    }


    @MainThread
    private static Clickable getSettingsAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.pushRenderable(new SettingsMenu());
            return ButtonResult.SUCCESS;
        };
    }

    private final ArrayList<UiButton> worldButtons = new ArrayList<>();
    private final UiButton playWorldButton, deleteWorldButton, optimizeWorldButton, confirmDeletionButton, cancelDeletionButton;
    private MenuInput<MainMenu> input;
}
