package game.player.inventory;

import core.assets.AssetManager;
import core.renderables.ButtonResult;
import core.renderables.Clickable;
import core.renderables.Renderable;
import core.renderables.TextField;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.utils.FileManager;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.server.Game;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.File;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class StructureTab extends Renderable implements InventoryTab {

    public StructureTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        filterTextField = new TextField(new Vector2f(0.275F, 0.1F), new Vector2f(0.0375F, 0.9F), UiMessages.STRUCTURE_NAME, this::reloadStructureButtons);
        structureButtonsContainer = new Renderable(new Vector2f(0.25F, 0.9F), new Vector2f(0.05F, 0.0F));
        structureButtonsContainer.setDoAutoFocusScaling(false);
        structureButtonsContainer.setScaleWithGuiSize(false);

        addRenderable(structureButtonsContainer);
        addRenderable(filterTextField);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        if (!reloadDisplay || toLoadStructureButton == null) return;

        reloadDisplay = false;
        removeRenderable(selectedStructureDisplay).delete();

        Vector2f sizeToParent = new Vector2f(0.7F, 0.7F * Window.getAspectRatio());
        Vector2f offsetToParent = new Vector2f(0.3F, 0.5F - sizeToParent.y * 0.5F);
        selectedStructureDisplay = new StructureDisplay(sizeToParent, offsetToParent, AssetManager.get(toLoadStructureButton.getStructure()));
        selectedStructureDisplay.setDoAutoFocusScaling(false);
        selectedStructureDisplay.setScaleWithGuiSize(false);
        addRenderable(selectedStructureDisplay);
    }

    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        if (!Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON)) lastCursorPos.set(pixelCoordinate);
        filterTextField.setFocused(filterTextField.containsPixelCoordinate(pixelCoordinate));
        structureButtonsContainer.hoverOver(pixelCoordinate);
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        if (selectedStructureDisplay == null) return;

        selectedStructureDisplay.rotate(new Vector2i(pixelCoordinate).sub(lastCursorPos));
        lastCursorPos.set(pixelCoordinate);
    }

    @Override
    public void resizeSelfTo(int width, int height) {
        if (selectedStructureDisplay == null) return;

        selectedStructureDisplay.setSizeToParent(0.7F, 0.7F * Window.getAspectRatio());
        selectedStructureDisplay.setOffsetToParent(0.3F, 0.5F - 0.7F * Window.getAspectRatio() * 0.5F);
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {
        if (structureButtonsContainer.containsPixelCoordinate(pixelCoordinate)) {
            InventoryInput input = Game.getPlayer().getInventory().getInput();
            float newScroll = Math.max((float) (input.structureScroll - yScroll * 0.05), 0.0F);
            moveStructureButtons(newScroll - input.structureScroll);
            input.structureScroll = newScroll;
        } else if (selectedStructureDisplay != null && selectedStructureDisplay.containsPixelCoordinate(pixelCoordinate))
            selectedStructureDisplay.changeZoom(yScroll > 0 ? 1.05F : 1 / 1.05F);
    }

    void reloadStructureButtons() {
        InventoryInput input = Game.getPlayer().getInventory().getInput();
        for (Renderable button : structureButtons) structureButtonsContainer.removeRenderable(button).delete();
        structureButtons.clear();

        int structureCount = 0;
        Vector2f sizeToParent = new Vector2f(1.0F, 0.05F);
        File[] structureFiles = FileManager.getChildren(new File("assets/structures"));
        String filterText = filterTextField.getText().toLowerCase();

        for (File structureFile : structureFiles) {
            if (structureFile == null || !structureFile.getName().toLowerCase().contains(filterText)) continue;
            String structureName = structureFile.getName();
            Vector2f offsetToParent = new Vector2f(0.0F, 1.0F - ++structureCount * 0.065F + input.structureScroll);

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            button.setAction(getButtonAction(button));
            button.setRimThicknessMultiplier(0.5F);
            button.setDoAutoFocusScaling(true);
            structureButtons.add(button);
            structureButtonsContainer.addRenderable(button);
        }
    }

    private void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
    }

    private Clickable getButtonAction(StructureSelectionButton selectionButton) {
        return (Vector2i pixelCoordinate, int _, int action) -> {
            if (action != GLFW_PRESS || !selectionButton.containsPixelCoordinate(pixelCoordinate)) return ButtonResult.IGNORE;
            reloadDisplay = true;
            toLoadStructureButton = selectionButton;
            return ButtonResult.SUCCESS;
        };
    }

    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final Renderable structureButtonsContainer;
    private final Vector2i lastCursorPos = new Vector2i();

    private final TextField filterTextField;
    private StructureDisplay selectedStructureDisplay;

    private boolean reloadDisplay = false;
    private StructureSelectionButton toLoadStructureButton;
}
