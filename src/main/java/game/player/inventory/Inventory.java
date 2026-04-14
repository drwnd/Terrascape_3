package game.player.inventory;

import core.assets.CoreTextures;
import core.renderables.*;

import core.rendering_api.Window;
import game.language.UiMessages;
import game.player.Hotbar;
import game.server.Game;
import game.server.generation.Structure;
import game.settings.FloatSettings;
import game.settings.KeySettings;

import game.settings.OptionSettings;
import game.settings.ToggleSettings;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static game.utils.Constants.AMOUNT_OF_MATERIALS;
import static org.lwjgl.glfw.GLFW.*;

public final class Inventory extends UiElement {

    public Inventory() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), CoreTextures.OVERLAY);
        input = new InventoryInput(this);

        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        ArrayList<CubeDisplay> cubeDisplays = getCubeDisplays();
        OptionToggle placeModeToggle = new OptionToggle(new Vector2f(0.125F, 0.1F), new Vector2f(0.025F, 0.9F), OptionSettings.PLACE_MODE, null, true);
        Toggle offsetToggle = new Toggle(new Vector2f(0.125F, 0.1F), new Vector2f(0.2F, 0.9F), ToggleSettings.OFFSET_FROM_GROUND, UiMessages.OFFSET_FROM_GROUND, true);

        Vector2f sizeToParent = new Vector2f(1.0F - TabOpenerButton.SIZE / Window.getAspectRatio(), 1.0F);
        Vector2f offsetToParent = new Vector2f(TabOpenerButton.SIZE / Window.getAspectRatio(), 0.0F);

        addRenderable(structureTab = new StructureTab(sizeToParent, offsetToParent));
        addRenderable(shapesTab = new ShapesTab(sizeToParent, offsetToParent));
        addRenderable(miscellaneousTab = new MiscellaneousTab(sizeToParent, offsetToParent));
        addRenderable(customShapeTab = new CustomShapeTab(sizeToParent, offsetToParent));

        shapesTab.addContents(cubeDisplays, placeModeToggle, offsetToggle);
        customShapeTab.addContents(cubeDisplays, placeModeToggle, offsetToggle);

        addRenderable(openTabButton = new TabOpenerButton(this, 0, shapesTab, UiMessages.SHAPES_TAB_NAME));
        addRenderable(new TabOpenerButton(this, 1, structureTab, UiMessages.STRUCTURE_TAB_NAME));
        addRenderable(new TabOpenerButton(this, 2, customShapeTab, UiMessages.CUSTOM_SHAPES_TAB_NAME));
        addRenderable(new TabOpenerButton(this, 3, miscellaneousTab, UiMessages.MISCELLANEOUS_TAB_NAME));

        openInventoryTab = shapesTab;
        openInventoryTab.setVisible(true);
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
        shapesTab.updateDisplayPositions();
    }

    public TabOpenerButton getOpenTabButton() {
        return openTabButton;
    }

    void setOpenTab(InventoryTab toOpenTab, TabOpenerButton button) {
        structureTab.setVisible(false);
        shapesTab.setVisible(false);
        miscellaneousTab.setVisible(false);
        customShapeTab.setVisible(false);

        openInventoryTab = toOpenTab;
        openInventoryTab.setVisible(true);

        openTabButton = button;
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


    private static ArrayList<CubeDisplay> getCubeDisplays() {
        long start = System.nanoTime();
        ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>(AMOUNT_OF_MATERIALS);
        Vector2f sizeToParent = new Vector2f(), offsetToParent = new Vector2f();

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            Structure structure = new Structure((byte) material);

            StructureDisplay display = new StructureDisplay(sizeToParent, offsetToParent, structure);
            display.setScalingFactor(FloatSettings.INVENTORY_ITEM_SCALING.value());
            display.setScaleWithGuiSize(false);

            cubeDisplays.add(new CubeDisplay(display, (byte) material));
        }
        System.out.printf("Build cube displays. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
        return cubeDisplays;
    }


    private final StructureTab structureTab;
    private final ShapesTab shapesTab;
    private final MiscellaneousTab miscellaneousTab;
    private final CustomShapeTab customShapeTab;

    private final InventoryInput input;

    private InventoryTab openInventoryTab;
    private TabOpenerButton openTabButton;
}
