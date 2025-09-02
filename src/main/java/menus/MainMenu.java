package menus;

import org.joml.Vector2f;
import org.joml.Vector2i;
import renderables.Renderable;
import renderables.TextElement;
import renderables.UiBackgroundElement;
import renderables.UiButton;
import rendering_api.Window;
import server.Game;

import java.io.File;
import java.util.ArrayList;

public final class MainMenu extends UiBackgroundElement {

    public MainMenu() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));
        Vector2f sizeToParent = new Vector2f(0.25f, 0.1f);

        UiButton closeApplicationButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.85f), Window::popRenderable);
        TextElement text = new TextElement(new Vector2f(0.05f, 0.5f), "Quit Game");
        closeApplicationButton.addRenderable(text);

        UiButton createNewWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.7f), getCreateWorldRunnable());
        text = new TextElement(new Vector2f(0.05f, 0.5f), "New World");
        createNewWorldButton.addRenderable(text);

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.55f), getSettingsRunnable());
        text = new TextElement(new Vector2f(0.05f, 0.5f), "Settings");
        settingsButton.addRenderable(text);

        playWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.4f));
        text = new TextElement(new Vector2f(0.05f, 0.5f));
        playWorldButton.addRenderable(text);
        playWorldButton.setVisible(false);

        deleteWorldButton = new UiButton(sizeToParent, new Vector2f(0.05f, 0.05f));
        text = new TextElement(new Vector2f(0.05f, 0.5f));
        deleteWorldButton.addRenderable(text);
        deleteWorldButton.setVisible(false);

        addRenderable(settingsButton);
        addRenderable(createNewWorldButton);
        addRenderable(closeApplicationButton);
        addRenderable(playWorldButton);
        addRenderable(deleteWorldButton);
    }

    public void moveWorldButtons(float movement) {
        Vector2f offset = new Vector2f(0, movement);
        for (Renderable renderable : worldButtons) renderable.move(offset);
    }

    public void setSelectedWorld(File saveFile) {
        playWorldButton.setAction(getPlayWorldRunnable(saveFile));
        deleteWorldButton.setAction(getDeleteWorldRunnable(saveFile, this));

        ((TextElement) playWorldButton.getChildren().getFirst()).setText("Play %s".formatted(saveFile.getName()));
        ((TextElement) deleteWorldButton.getChildren().getFirst()).setText("Delete %s".formatted(saveFile.getName()));

        playWorldButton.setVisible(true);
        deleteWorldButton.setVisible(true);
    }

    @Override
    public void setOnTop() {
        // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
        input = new menus.MainMenuInput(this);
        Window.setInput(input);
        createWorldButtons();
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

        if (!buttonFound) {
            playWorldButton.setVisible(false);
            deleteWorldButton.setVisible(false);
        }
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

    private static Runnable getSettingsRunnable() {
        return () -> Window.pushRenderable(new SettingsMenu());
    }

    private static Runnable getCreateWorldRunnable() {
        return () -> Window.pushRenderable(new WorldCreationMenu());
    }

    private static Runnable getPlayWorldRunnable(File saveFile) {
        return () -> Game.play(saveFile);
    }

    private static Runnable getDeleteWorldRunnable(File saveFile, MainMenu menu) {
        return () -> {
            saveFile.delete();
            menu.createWorldButtons();
            menu.playWorldButton.setVisible(false);
            menu.deleteWorldButton.setVisible(false);
        };
    }


    private final ArrayList<UiButton> worldButtons = new ArrayList<>();
    private final UiButton playWorldButton, deleteWorldButton;
    private menus.MainMenuInput input;  // IDK why but sometimes it doesn't find MainMenuInput without the package declaration
}
