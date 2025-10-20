package game.menus;

import core.renderables.*;
import core.utils.FileManager;
import core.languages.Language;
import core.languages.UiMessage;
import core.rendering_api.Window;

import game.server.Game;
import game.server.WorldOptimizer;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public final class MainMenu extends UiBackgroundElement {

    public MainMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        Vector2f sizeToParent = new Vector2f(0.25F, 0.1F);

        UiButton closeApplicationButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.85F), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.QUIT_GAME);
        closeApplicationButton.addRenderable(text);

        UiButton createNewWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.725F), getCreateWorldAction());
        text = new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.NEW_WORLD);
        createNewWorldButton.addRenderable(text);

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.6F), getSettingsAction());
        settingsButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.SETTINGS));

        playWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.475F));
        playWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F)));

        deleteWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.05F));
        deleteWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F)));

        optimizeWorldButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.35F));
        optimizeWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 2.0F / 3.0F)));
        optimizeWorldButton.addRenderable(new TextElement(new Vector2f(0.05F, 1.0F / 3.0F), UiMessage.WORLD_OPTIMIZER_TIME_WARNING));

        confirmDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.175F));
        confirmDeletionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.CONFIRM_DELETE_WORLD, Color.RED));

        cancelDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05F, 0.05F));
        cancelDeletionButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.KEEP_WORLD, Color.GREEN));
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

    public void moveWorldButtons(float movement) {
        Vector2f offset = new Vector2f(0, movement);
        for (Renderable renderable : worldButtons) renderable.move(offset);
    }


    @Override
    public void setOnTop() {
        // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
        input = new game.menus.MainMenuInput(this);
        Window.setInput(input);
        createWorldButtons();
        hideWorldSpecificButtons();
    }

    @Override
    public void clickOn(Vector2i pixelCoordinate, int mouseButton, int action) {
        boolean buttonFound = false;
        for (Renderable button : getChildren())
            if (button.isVisible() && button.containsPixelCoordinate(pixelCoordinate)) {
                button.clickOn(pixelCoordinate, mouseButton, action);
                buttonFound = true;
                break;
            }

        if (!buttonFound) hideWorldSpecificButtons();
    }


    private void setSelectedWorld(File saveFile) {
        hideWorldSpecificButtons();

        playWorldButton.setAction(getPlayWorldAction(saveFile));
        deleteWorldButton.setAction(getDeleteWorldAction(saveFile));
        optimizeWorldButton.setAction(getOptimizeWorldAction(saveFile));

        ((TextElement) playWorldButton.getChildren().getFirst()).setText(Language.getUiMessage(UiMessage.PLAY_WORLD).formatted(saveFile.getName()));
        ((TextElement) deleteWorldButton.getChildren().getFirst()).setText(Language.getUiMessage(UiMessage.DELETE_WORLD).formatted(saveFile.getName()));
        ((TextElement) optimizeWorldButton.getChildren().getFirst()).setText(Language.getUiMessage(UiMessage.OPTIMIZE_WORLD).formatted(saveFile.getName()));

        playWorldButton.setVisible(true);
        deleteWorldButton.setVisible(true);
        optimizeWorldButton.setVisible(true);
    }

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

    private void createWorldButtons() {
        getChildren().removeAll(worldButtons);

        File[] savedWorlds = getSavedWorlds();
        for (int index = 0; index < savedWorlds.length; index++) {
            File saveFile = savedWorlds[index];

            UiButton button = getPlayWorldButton(index, saveFile);

            addRenderable(button);
            worldButtons.add(button);
        }
    }

    private UiButton getPlayWorldButton(int index, File saveFile) {
        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);
        Vector2f offsetToParent = new Vector2f(0.35F, 1.0F - 0.15F * (index + 1) + input.getScroll());

        UiButton button = new UiButton(sizeToParent, offsetToParent, () -> setSelectedWorld(saveFile));

        TextElement text = new TextElement(new Vector2f(0.05F, 0.5F));
        text.setText(saveFile.getName());
        button.addRenderable(text);

        return button;
    }

    private Clickable getDeleteWorldAction(File saveFile) {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            hideWorldSpecificButtons();

            confirmDeletionButton.setAction(getConfirmDeletionAction(saveFile));
            confirmDeletionButton.setVisible(true);
            cancelDeletionButton.setVisible(true);
        };
    }

    private Clickable getConfirmDeletionAction(File saveFile) {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;

            FileManager.delete(saveFile);
            createWorldButtons();
            hideWorldSpecificButtons();
        };
    }

    private Clickable getOptimizeWorldAction(File saveFile) {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            WorldOptimizer.optimize(saveFile);
            hideWorldSpecificButtons();
        };
    }


    private static Clickable getSettingsAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.pushRenderable(new SettingsMenu());
        };
    }

    private static Clickable getCreateWorldAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.pushRenderable(new WorldCreationMenu());
        };
    }

    private static Clickable getPlayWorldAction(File saveFile) {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Game.play(saveFile);
        };
    }

    private final ArrayList<UiButton> worldButtons = new ArrayList<>();
    private final UiButton playWorldButton, deleteWorldButton, optimizeWorldButton, confirmDeletionButton, cancelDeletionButton;
    private game.menus.MainMenuInput input;  // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
}
