package game.player.inventory;

import core.assets.AssetManager;
import core.renderables.*;
import core.rendering_api.Input;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.server.Game;

import core.utils.MainThread;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class StructureTab extends Renderable implements InventoryTab {

    @MainThread
    public StructureTab(Vector2f sizeToParent, Vector2f offsetToParent, InventoryInput input) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        filterTextField = new TextField(new Vector2f(0.3F, 0.1F), new Vector2f(0.0375F, 0.9F), UiMessages.STRUCTURE_NAME, this::reloadStructureButtons);
        structureButtonsContainer = new StructureButtonsContainer(new Vector2f(0.3F, 0.9F), new Vector2f(0.0375F, 0.0F));
        structureButtonsContainer.setDoAutoFocusScaling(false);
        structureButtonsContainer.setScaleWithGuiSize(false);
        structureButtonsContainer.addRenderable(new StructureScroller(input, this));

        addRenderable(structureButtonsContainer);
        addRenderable(filterTextField);
    }

    @Override
    @MainThread
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        if (!reloadDisplay || toLoadStructureButton == null) return;

        reloadDisplay = false;
        removeRenderable(selectedStructureDisplay).delete();

        Vector2f sizeToParent = new Vector2f(0.6625F, 0.6625F * Window.getAspectRatio());
        Vector2f offsetToParent = new Vector2f(0.3375F, 0.5F - sizeToParent.y * 0.5F);
        selectedStructureDisplay = new StructureDisplay(sizeToParent, offsetToParent, AssetManager.get(toLoadStructureButton.getStructure()));
        selectedStructureDisplay.setDoAutoFocusScaling(false);
        selectedStructureDisplay.setScaleWithGuiSize(false);
        addRenderable(selectedStructureDisplay);
    }

    @Override
    @MainThread
    public void hoverOver(Vector2i pixelCoordinate) {
        if (!Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON)) lastCursorPos.set(pixelCoordinate);
        filterTextField.setFocused(filterTextField.containsPixelCoordinate(pixelCoordinate));
        structureButtonsContainer.hoverOver(pixelCoordinate);
    }

    @Override
    @MainThread
    public void dragOver(Vector2i pixelCoordinate) {
        if (selectedStructureDisplay == null || selectedDraggable != null) return;

        selectedStructureDisplay.rotate(new Vector2i(pixelCoordinate).sub(lastCursorPos));
        lastCursorPos.set(pixelCoordinate);
    }

    @Override
    @MainThread
    public void resizeSelfTo(int width, int height) {
        if (selectedStructureDisplay == null) return;

        selectedStructureDisplay.setSizeToParent(0.6625F, 0.6625F * Window.getAspectRatio());
        selectedStructureDisplay.setOffsetToParent(0.3375F, 0.5F - 0.6625F * Window.getAspectRatio() * 0.5F);
    }

    @Override
    @MainThread
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    @Override
    @MainThread
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {
        if (structureButtonsContainer.containsPixelCoordinate(pixelCoordinate)) {
            InventoryInput input = Game.getPlayer().getInventory().getInput();
            float maxScroll = getMaxScroll(pixelCoordinate);
            float newScroll = maxScroll <= 0.0F ? 0.0F : Math.clamp((float) (input.structureScroll - yScroll * 0.05), 0.0F, maxScroll);
            moveStructureButtons(newScroll - input.structureScroll);
            input.structureScroll = newScroll;
        } else if (selectedStructureDisplay != null && selectedStructureDisplay.containsPixelCoordinate(pixelCoordinate))
            selectedStructureDisplay.changeZoom(yScroll > 0 ? 1.05F : 1 / 1.05F);
    }

    @Override
    @MainThread
    public float getMaxScroll(Vector2i pixelCoordinate) {
        if (structureButtonsContainer.containsPixelCoordinate(pixelCoordinate)) return getMaxScroll(structureButtons);
        return Float.POSITIVE_INFINITY;
    }

    @MainThread
    void reloadStructureButtons() {
        InventoryInput input = Game.getPlayer().getInventory().getInput();
        for (Renderable button : structureButtons) structureButtonsContainer.removeRenderable(button).delete();
        structureButtons.clear();

        int structureCount = 0;
        Vector2f sizeToParent = new Vector2f(0.75F, 0.05F);
        ArrayList<Path> filePaths = AssetManager.getAssetFilePathsInFolder("structures");
        String filterText = filterTextField.getText().toLowerCase();

        for (Path filepath : filePaths) {
            File structureFile = filepath.toFile();
            if (!structureFile.getName().toLowerCase().contains(filterText)) continue;
            String structureName = structureFile.getName();
            Vector2f offsetToParent = new Vector2f(0.25F, 1.0F - ++structureCount * 0.065F + input.structureScroll);

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            button.setAction(getButtonAction(button));
            button.setRimThicknessMultiplier(0.5F);
            button.setDoAutoFocusScaling(true);
            structureButtons.add(button);
            structureButtonsContainer.addRenderable(button);
        }
    }

    @MainThread
    private void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
    }

    @MainThread
    private Clickable getButtonAction(StructureSelectionButton selectionButton) {
        return (Vector2i pixelCoordinate, int _, int action) -> {
            if (action != GLFW_PRESS || !selectionButton.containsPixelCoordinate(pixelCoordinate)) return ButtonResult.IGNORE;
            reloadDisplay = true;
            toLoadStructureButton = selectionButton;
            return ButtonResult.SUCCESS;
        };
    }

    private static float getMaxScroll(ArrayList<StructureSelectionButton> structureButtons) {
        return structureButtons.size() * 0.065F - 1;
    }

    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final Renderable structureButtonsContainer;
    private final Vector2i lastCursorPos = new Vector2i();

    private final TextField filterTextField;
    private StructureDisplay selectedStructureDisplay;

    private boolean reloadDisplay = false;
    private StructureSelectionButton toLoadStructureButton;

    private static class StructureScroller extends SideScroller {

        @MainThread
        private StructureScroller(InventoryInput input, StructureTab structureTab) {
            super(new Vector2f(0.2F, 0.9F), new Vector2f(0.025F, 0.05F), input);
            this.structureTab = structureTab;
        }

        @Override
        @MainThread
        protected void renderSelf(Vector2f position, Vector2f size) {
            InventoryInput input = (InventoryInput) this.input;
            super.renderSelf(position, size);
            float fraction = 1 - input.structureScroll / getMaxScroll(structureTab.structureButtons);
            slider.setOffsetToParent(0.0F, fraction - slider.getSizeToParent().y * 0.5F);
            slider.setSizeToParent(1.0F, 0.5625F * Window.getAspectRatio() * getAspectRatio());
        }

        @Override
        @MainThread
        protected void applyScrolling(Vector2i cursorPos, Vector2f position, Vector2f size) {
            InventoryInput input = (InventoryInput) this.input;
            float fraction = 1 - (cursorPos.y - position.y) / size.y;
            fraction = Math.clamp(fraction, 0.0F, 1.0F);
            float newScroll = fraction * getMaxScroll(structureTab.structureButtons);
            structureTab.moveStructureButtons(newScroll - input.structureScroll);
            input.structureScroll = newScroll;
            slider.setOffsetToParent(0.0F, fraction - slider.getSizeToParent().y * 0.5F);
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        private final StructureTab structureTab;
    }

    private static class StructureButtonsContainer extends Renderable {

        @MainThread
        private StructureButtonsContainer(Vector2f sizeToParent, Vector2f offsetToParent) {
            super(sizeToParent, offsetToParent);
        }

        @Override
        @MainThread
        public void hoverOver(Vector2i pixelCoordinate) {
            for (Renderable renderable : getChildren()) {
                if (!renderable.isVisible()) continue;
                renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
            }
        }
    }
}
