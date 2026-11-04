package game.player.inventory;

import core.utils.MainThread;
import core.utils.StringGetter;

import game.items.Item;

import org.joml.Vector2i;

public interface InventoryTab {

    Item getSelectedItem(Vector2i pixelCoordinate);

    void setVisible(boolean visible);

    void handleScroll(Vector2i pixelCoordinate, double yScroll);

    float getMaxScroll(Vector2i pixelCoordinate);

    enum Types {
        SHAPES, STRUCTURES, CUSTOM_SHAPES, MISCELLANEOUS;

        @MainThread
        public TabOpenerButton createButton(Inventory inventory, int index, InventoryTab toOpenTab, StringGetter name) {
            return button = new TabOpenerButton(inventory, index, toOpenTab, name, this);
        }

        public TabOpenerButton getButton() {
            return button;
        }

        private TabOpenerButton button;
    }
}
