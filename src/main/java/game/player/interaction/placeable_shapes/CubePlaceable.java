package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.utils.Saver;

import game.player.interaction.Placeable;
import game.player.interaction.ShapePlaceable;

import java.util.Arrays;
import java.util.List;

public final class CubePlaceable extends ShapePlaceable {

    public CubePlaceable(byte material) {
        super(material);
    }

    public void save(Placeable placeable, Saver<?> saver) {
        saver.saveByte((byte) 1);
        saver.saveByte(((CubePlaceable) placeable).getMaterial());
    }

    public static CubePlaceable load(Saver<?> saver) {
        return new CubePlaceable(saver.loadByte());
    }

    @Override
    public List<Slider<?>> settings() {
        return List.of();
    }

    @Override
    public ShapePlaceable copyWithMaterial(byte material) {
        return new CubePlaceable(material);
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        Arrays.fill(bitMap, -1L);
        if (sideLength == 1) bitMap[0] = 0x8L;
        if (sideLength == 2) bitMap[0] = 0xFFL;
    }
}
