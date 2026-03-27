package game.player.inventory;

import game.player.interaction.Placeable;
import org.joml.Vector2i;

public interface InventoryTab {

    Placeable getSelectedPlaceable(Vector2i pixelCoordinate);

    void setVisible(boolean visible);

}
