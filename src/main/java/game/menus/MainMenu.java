package game.menus;

import core.renderables.*;
import core.utils.FileManager;
import core.language.Language;
import core.language.CoreUiMessages;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.server.Game;
import game.server.WorldOptimizer;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class MainMenu extends UiBackgroundElement {

    /**
     * Constructs the main menu with its various UI components.
     * Initializes buttons for quitting, world creation, settings, and world management.
     */
    public MainMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        Vector2f sizeToParent = new Vector2f(0.25F, 0.1F);

        UiButton closeApplicationButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.QUIT_GAME);
        closeApplicationButton.addRenderable(text);

        UiButton createNewWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.725F), getCreateWorldAction());
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

    /**
     * Moves all world selection buttons by a specified vertical offset.
     *
     * @param movement the vertical distance to move the buttons
     */
    public void moveWorldButtons(float movement) {
        Vector2f offset = new Vector2f(0, movement);
        for (Renderable renderable : worldButtons) renderable.move(offset);
    }


    /**
     * Called when the main menu becomes the active renderable.
     * Sets up input handling and refreshes world buttons.
     */
    @Override
    public void setOnTop() {
        // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
        input = new game.menus.MainMenuInput(this);
        Window.setInput(input);
        createWorldButtons();
        hideWorldSpecificButtons();
    }

    /**
     * Handles mouse clicks on the main menu.
     * Forwards clicks to child elements and manages world-specific button visibility.
     *
     * @param pixelCoordinate the coordinates of the click in pixels
     * @param mouseButton     the mouse button that was clicked
     * @param action          the click action (press/release)
     * @return always true to indicate the click was handled
     */
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


    /**
     * Updates the UI to show world-specific actions for a selected save file.
     *
     * @param saveFile the file representing the selected world
     */
    private void setSelectedWorld(File saveFile) {
        hideWorldSpecificButtons();

        playWorldButton.setAction(getPlayWorldAction(saveFile));
        deleteWorldButton.setAction(getDeleteWorldAction(saveFile));
        optimizeWorldButton.setAction(getOptimizeWorldAction(saveFile));

        playWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.PLAY_WORLD).formatted(saveFile.getName()));
        deleteWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.DELETE_WORLD).formatted(saveFile.getName()));
        optimizeWorldButton.firstChildOf(TextElement.class).setText(Language.getTranslation(UiMessages.OPTIMIZE_WORLD).formatted(saveFile.getName()));

        playWorldButton.setVisible(true);
        deleteWorldButton.setVisible(true);
        optimizeWorldButton.setVisible(true);
    }

    /**
     * Hides all buttons related to specific world actions.
     */
    private void hideWorldSpecificButtons() {
        playWorldButton.setVisible(false);
        deleteWorldButton.setVisible(false);
        optimizeWorldButton.setVisible(false);
        confirmDeletionButton.setVisible(false);
        cancelDeletionButton.setVisible(false);
    }

    public static File[] getSavedWorlds() {
        return FileManager.getChildren(new File("saves"));
    }

    /**
     * Scans for saved world folders and creates buttons for each.
     */
    private void createWorldButtons() {
        for (Renderable worldButton : worldButtons) removeRenderable(worldButton).delete();

        File[] savedWorlds = getSavedWorlds();
        for (int index = 0; index < savedWorlds.length; index++) {
            File saveFile = savedWorlds[index];

            UiButton button = getPlayWorldButton(index, saveFile);

            addRenderable(button);
            worldButtons.add(button);
        }
    }

    /**
     * Creates a button for a specific saved world.
     *
     * @param index    the index of the world in the list
     * @param saveFile the save file for the world
     * @return a new UiButton for selecting the world
     */
    private UiButton getPlayWorldButton(int index, File saveFile) {
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * (index + 1) + input.getScroll());

        UiButton button = new UiButton(sizeToParent, offsetToParent, () -> setSelectedWorld(saveFile));

        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F));
        text.setText(saveFile.getName());
        button.addRenderable(text);

        return button;
    }

    /**
     * Returns the action for deleting a specific world.
     *
     * @param saveFile the world save file to delete
     * @return a Clickable action that shows confirmation buttons
     */
    private Clickable getDeleteWorldAction(File saveFile) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            hideWorldSpecificButtons();

            confirmDeletionButton.setAction(getConfirmDeletionAction(saveFile));
            confirmDeletionButton.setVisible(true);
            cancelDeletionButton.setVisible(true);
            return ButtonResult.SUCCESS;
        };
    }

    /**
     * Returns the action for confirming world deletion.
     *
     * @param saveFile the world save file to delete
     * @return a Clickable action that performs the deletion
     */
    private Clickable getConfirmDeletionAction(File saveFile) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            FileManager.delete(saveFile);
            createWorldButtons();
            hideWorldSpecificButtons();
            return ButtonResult.SUCCESS;
        };
    }

    /**
     * Returns the action for optimizing a world.
     *
     * @param saveFile the world save file to optimize
     * @return a Clickable action that starts the optimization process
     */
    private Clickable getOptimizeWorldAction(File saveFile) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            WorldOptimizer.optimize(saveFile);
            hideWorldSpecificButtons();
            return ButtonResult.SUCCESS;
        };
    }


    /**
     * Returns the action for opening the settings menu.
     *
     * @return a Clickable action that pushes the SettingsMenu
     */
    private static Clickable getSettingsAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.pushRenderable(new SettingsMenu());
            return ButtonResult.SUCCESS;
        };
    }

    /**
     * Returns the action for opening the world creation menu.
     *
     * @return a Clickable action that pushes the WorldCreationMenu
     */
    private static Clickable getCreateWorldAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.pushRenderable(new WorldCreationMenu());
            return ButtonResult.SUCCESS;
        };
    }

    /**
     * Returns the action for playing a specific world.
     *
     * @param saveFile the world save file to play
     * @return a Clickable action that starts the game with the selected world
     */
    private static Clickable getPlayWorldAction(File saveFile) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Game.play(saveFile);
            return ButtonResult.SUCCESS;
        };
    }

    private final ArrayList<UiButton> worldButtons = new ArrayList<>();
    private final UiButton playWorldButton, deleteWorldButton, optimizeWorldButton, confirmDeletionButton, cancelDeletionButton;
    private game.menus.MainMenuInput input;  // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
}
