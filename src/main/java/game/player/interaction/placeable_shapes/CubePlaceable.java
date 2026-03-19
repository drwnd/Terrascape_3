package game.player.interaction.placeable_shapes;

import core.renderables.Slider;

import game.player.interaction.ShapePlaceable;

import java.util.Arrays;
import java.util.List;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(material);
    }

    @Override
    public List<Slider<?>> settings() {
        return List.of();
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        Arrays.fill(bitMap, -1L);
        if (sideLength == 1) bitMap[0] = 0x8L;
        if (sideLength == 2) bitMap[0] = 0xFFL;
    }
}
