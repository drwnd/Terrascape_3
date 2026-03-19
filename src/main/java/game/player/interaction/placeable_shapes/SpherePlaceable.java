package game.player.interaction.placeable_shapes;

import core.renderables.Slider;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;

import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.server.MaterialsData;

import org.joml.Vector2f;

import java.util.List;

import static game.utils.Constants.CHUNK_SIZE;

public final class SpherePlaceable extends ShapePlaceable {

    public SpherePlaceable(byte material) {
        super(material);
    }

    @Override
    public List<Slider<?>> settings() {
        Vector2f zero = new Vector2f();
        return List.of(
                new Slider<>(zero, zero, innerRadius, UiMessages.INNER_RADIUS),
                new Slider<>(zero, zero, exponent, UiMessages.DISTANCE_EXPONENT));
    }

    @Override
    public ShapePlaceable copyWithMaterial(byte material) {
        SpherePlaceable placeable = new SpherePlaceable(material);
        placeable.innerRadius.setValue(innerRadius.value());
        placeable.exponent.setValue(exponent.value());
        return placeable;
    }

    @Override
    protected void fillBitMap(long[] bitMap, int sideLength) {
        int outerRadius = sideLength / 2;

        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, outerRadius)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    private boolean isInside(int x, int y, int z, int outerRadius) {
        double distanceX = Math.pow(Math.abs(x - outerRadius + 0.5), exponent.value());
        double distanceY = Math.pow(Math.abs(y - outerRadius + 0.5), exponent.value());
        double distanceZ = Math.pow(Math.abs(z - outerRadius + 0.5), exponent.value());
        double distance = distanceX + distanceY + distanceZ;

        return distance <= Math.pow(outerRadius, exponent.value()) && distance >= Math.pow(innerRadius.value(), exponent.value());
    }

    private final StandAloneIntSetting innerRadius = new StandAloneIntSetting(0, CHUNK_SIZE / 2, 0);
    private final StandAloneFloatSetting exponent = new StandAloneFloatSetting(0.0F, 20.0F, 2.0F, 0.1F);
}
