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
 * Creates a new MainMenu instance.
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
 * Performs move world buttons.
 *
 * @param movement parameter
 */
    public void moveWorldButtons(float movement) {
        Vector2f offset = new Vector2f(0, movement);
        for (Renderable renderable : worldButtons) renderable.move(offset);
    }


/**
 * Sets on top.
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
 * Performs click on.
 *
 * @param pixelCoordinate parameter
 * @param mouseButton parameter
 * @param action parameter
 * @return true if the condition holds
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
 * Sets selected world.
 *
 * @param saveFile parameter
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
 * Performs hide world specific buttons.
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
 * Creates world buttons.
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
 * Returns the play world button.
 *
 * @param index X coordinate in local block coordinates
 * @param saveFile parameter
 * @return result
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
 * Returns the delete world action.
 *
 * @param saveFile parameter
 * @return result
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
 * Returns the confirm deletion action.
 *
 * @param saveFile parameter
 * @return result
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
 * Returns the optimize world action.
 *
 * @param saveFile parameter
 * @return result
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
 * Returns the settings action.
 * @return result
 */
    private static Clickable getSettingsAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.pushRenderable(new SettingsMenu());
            return ButtonResult.SUCCESS;
        };
    }

/**
 * Returns the create world action.
 * @return result
 */
    private static Clickable getCreateWorldAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.pushRenderable(new WorldCreationMenu());
            return ButtonResult.SUCCESS;
        };
    }

/**
 * Returns the play world action.
 *
 * @param saveFile parameter
 * @return result
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
