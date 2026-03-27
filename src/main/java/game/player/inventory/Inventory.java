package game.player.inventory;

import core.assets.CoreTextures;
import core.renderables.*;

import game.language.UiMessages;
import game.player.Hotbar;
import game.server.Game;
import game.settings.KeySettings;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class Inventory extends UiElement {

    public Inventory() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), CoreTextures.OVERLAY);
        input = new InventoryInput(this);

        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        Vector2f sizeToParent = new Vector2f(1.0F - TabOpenerButton.SIZE, 1.0F);
        Vector2f offsetToParent = new Vector2f(TabOpenerButton.SIZE, 0.0F);

        structureTab = new StructureTab(sizeToParent, offsetToParent);
        placeablesTab = new PlaceablesTab(sizeToParent, offsetToParent);
        miscellaneousTab = new MiscellaneousTab(sizeToParent, offsetToParent);

        addRenderable(structureTab);
        addRenderable(placeablesTab);
        addRenderable(miscellaneousTab);

        openInventoryTab = placeablesTab;
        openInventoryTab.setVisible(true);

        addRenderable(new TabOpenerButton(0, getTabOpenerButtonAction(placeablesTab), UiMessages.PLACEABLE_TAB_NAME));
        addRenderable(new TabOpenerButton(1, getTabOpenerButtonAction(structureTab), UiMessages.STRUCTURE_TAB_NAME));
        addRenderable(new TabOpenerButton(2, getTabOpenerButtonAction(miscellaneousTab), UiMessages.MISCELLANEOUS_TAB_NAME));
    }

    public InventoryInput getInput() {
        return input;
    }

    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        super.hoverOver(pixelCoordinate);
        for (Renderable renderable : getChildren()) if (renderable.isVisible()) renderable.hoverOver(pixelCoordinate);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!isVisible()) return;

        structureTab.reloadStructureButtons();
        placeablesTab.updateDisplayPositions();
    }

    void handleInput(int button, int action, Vector2i pixelCoordinate) {
        if (action != GLFW_PRESS || !isVisible()) return;
        Hotbar hotbar = Game.getPlayer().getHotbar();

        if (button == KeySettings.HOTBAR_SLOT_1.keybind()) hotbar.setContent(0, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_2.keybind()) hotbar.setContent(1, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_3.keybind()) hotbar.setContent(2, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_4.keybind()) hotbar.setContent(3, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_5.keybind()) hotbar.setContent(4, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_6.keybind()) hotbar.setContent(5, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_7.keybind()) hotbar.setContent(6, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_8.keybind()) hotbar.setContent(7, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_9.keybind()) hotbar.setContent(8, openInventoryTab.getSelectedPlaceable(pixelCoordinate));
    }

    void handleScroll(Vector2i pixelCoordinate, double yScroll) {
        openInventoryTab.handleScroll(pixelCoordinate, yScroll);
    }

    private Clickable getTabOpenerButtonAction(InventoryTab toOpenTab) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            structureTab.setVisible(false);
            placeablesTab.setVisible(false);
            miscellaneousTab.setVisible(false);

            openInventoryTab = toOpenTab;
            openInventoryTab.setVisible(true);
        };
    }

    final StructureTab structureTab;
    final PlaceablesTab placeablesTab;
    final MiscellaneousTab miscellaneousTab;

    private final InventoryInput input;

    private InventoryTab openInventoryTab;
}
