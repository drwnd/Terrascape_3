package game.menus;

import org.joml.Vector2f;
import org.joml.Vector2i;
import core.renderables.Renderable;
import core.renderables.TextElement;
import core.renderables.UiBackgroundElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import game.server.Game;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public final class MainMenu extends UiBackgroundElement {

    public MainMenu() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));
        Vector2f sizeToParent = new Vector2f(0.25f, 0.1f);

        UiButton closeApplicationButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.85f), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05f, 0.5f), "Quit Game");
        closeApplicationButton.addRenderable(text);

        UiButton createNewWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.725f), getCreateWorldRunnable());
        text = new TextElement(new Vector2f(0.05f, 0.5f), "New World");
        createNewWorldButton.addRenderable(text);

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.6f), getSettingsRunnable());
        settingsButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), "Settings"));

        playWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.475f));
        playWorldButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f)));

        deleteWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.05f));
        deleteWorldButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f)));

        confirmDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.3f));
        confirmDeletionButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), "Delete world forever...  ;(", Color.RED));

        cancelDeletionButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.175f));
        cancelDeletionButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), "Keep World!  :)", Color.GREEN));
        cancelDeletionButton.setAction(this::hideWorldSpecificButtons);

        hideWorldSpecificButtons();

        addRenderable(settingsButton);
        addRenderable(createNewWorldButton);
        addRenderable(closeApplicationButton);
        addRenderable(playWorldButton);
        addRenderable(deleteWorldButton);
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
        playWorldButton.setAction(getPlayWorldRunnable(saveFile));
        deleteWorldButton.setAction(getDeleteWorldRunnable(saveFile));

        ((TextElement) playWorldButton.getChildren().getFirst()).setText("Play %s".formatted(saveFile.getName()));
        ((TextElement) deleteWorldButton.getChildren().getFirst()).setText("Delete %s".formatted(saveFile.getName()));

        playWorldButton.setVisible(true);
        deleteWorldButton.setVisible(true);
    }

    private void hideWorldSpecificButtons() {
        playWorldButton.setVisible(false);
        deleteWorldButton.setVisible(false);
        confirmDeletionButton.setVisible(false);
        cancelDeletionButton.setVisible(false);
    }

    public static File[] getSavedWorlds() {
        File savesFile = new File("saves");
        if (!savesFile.exists()) savesFile.mkdir();
        return savesFile.listFiles();
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
        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);
        Vector2f offsetToParent = new Vector2f(0.35f, 1.0f - 0.15f * (index + 1) + input.getScroll());

        UiButton button = new UiButton(sizeToParent, offsetToParent, () -> setSelectedWorld(saveFile));

        TextElement text = new TextElement(new Vector2f(0.05f, 0.5f));
        text.setText(saveFile.getName());
        button.addRenderable(text);

        return button;
    }

    private Runnable getDeleteWorldRunnable(File saveFile) {
        return () -> {
            confirmDeletionButton.setAction(getConfirmDeletionRunnable(saveFile));
            confirmDeletionButton.setVisible(true);
            cancelDeletionButton.setVisible(true);
        };
    }

    private Runnable getConfirmDeletionRunnable(File saveFile) {
        return () -> {
            delete(saveFile);
            createWorldButtons();
            hideWorldSpecificButtons();
        };
    }


    private static Runnable getSettingsRunnable() {
        return () -> Window.pushRenderable(new SettingsMenu());
    }

    private static Runnable getCreateWorldRunnable() {
        return () -> Window.pushRenderable(new WorldCreationMenu());
    }

    private static Runnable getPlayWorldRunnable(File saveFile) {
        return () -> Game.play(saveFile);
    }

    private static void delete(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }


    private final ArrayList<UiButton> worldButtons = new ArrayList<>();
    private final UiButton playWorldButton, deleteWorldButton, confirmDeletionButton, cancelDeletionButton;
    private game.menus.MainMenuInput input;  // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
}
