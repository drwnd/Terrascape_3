package game.player.interaction;

import core.settings.Setting;

import java.util.Arrays;
import java.util.List;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(material);
    }

    @Override
    public List<Setting> settings() {
        return List.of();
    }

    @Override
    void fillBitMap(long[] bitMap, int sideLength) {
        Arrays.fill(bitMap, -1L);
        if (sideLength == 1) bitMap[0] = 0x8L;
        if (sideLength == 2) bitMap[0] = 0xFFL;
    }
}
