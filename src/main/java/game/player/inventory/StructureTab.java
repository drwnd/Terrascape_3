package game.player.inventory;

import core.renderables.Renderable;
import core.renderables.TextField;
import core.utils.FileManager;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.player.rendering.StructureSelectionButton;
import game.server.Game;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.File;
import java.util.ArrayList;

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
    public void hoverOver(Vector2i pixelCoordinate) {
        if (structureButtonsContainer.containsPixelCoordinate(pixelCoordinate)) structureButtonsContainer.hoverOver(pixelCoordinate);
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
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
            Vector2f offsetToParent = new Vector2f(0.0F, 1.0F - ++structureCount * 0.065F + input.getStructureScroll());

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            button.setRimThicknessMultiplier(0.5F);
            button.setDoAutoFocusScaling(true);
            structureButtons.add(button);
            structureButtonsContainer.addRenderable(button);
        }
    }
    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final Renderable structureButtonsContainer;

    private final TextField filterTextField;
}
