package game.utils;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import org.joml.Vector3f;

import static game.utils.Constants.*;

public final class Position implements Distanceable {

    public long longX, longY, longZ;
    public float fractionX, fractionY, fractionZ;


    public void save(Saver<?> saver) {
        saver.saveLong(longX);
        saver.saveLong(longY);
        saver.saveLong(longZ);
        saver.saveFloat(fractionX);
        saver.saveFloat(fractionY);
        saver.saveFloat(fractionZ);
    }

    public static Position load(Saver<?> saver) {
        return new Position(saver.loadLong(), saver.loadLong(), saver.loadLong(), saver.loadFloat(), saver.loadFloat(), saver.loadFloat());
    }


    public Position() {
        this.longX = 0L;
        this.longY = 0L;
        this.longZ = 0L;
        this.fractionX = 0.0F;
        this.fractionY = 0.0F;
        this.fractionZ = 0.0F;
    }

    public Position(long intX, long intY, long intZ, float fractionX, float fractionY, float fractionZ) {
        this.longX = intX;
        this.longY = intY;
        this.longZ = intZ;
        this.fractionX = fractionX;
        this.fractionY = fractionY;
        this.fractionZ = fractionZ;
    }

    public Position(Vector3l longPosition, Vector3f fractionPosition) {
        this.longX = longPosition == null ? 0L : longPosition.x;
        this.longY = longPosition == null ? 0L : longPosition.y;
        this.longZ = longPosition == null ? 0L : longPosition.z;
        this.fractionX = fractionPosition == null ? 0.0F : fractionPosition.x;
        this.fractionY = fractionPosition == null ? 0.0F : fractionPosition.y;
        this.fractionZ = fractionPosition == null ? 0.0F : fractionPosition.z;
    }

    public Position(Position position) {
        this.longX = position.longX;
        this.longY = position.longY;
        this.longZ = position.longZ;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;
    }

    public Position set(Position position) {
        this.longX = position.longX;
        this.longY = position.longY;
        this.longZ = position.longZ;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;

        return this;
    }

    public Position add(float x, float y, float z) {
        fractionX += x;
        fractionY += y;
        fractionZ += z;

        longX = longX + MathUtils.floor(fractionX);
        longY = longY + MathUtils.floor(fractionY);
        longZ = longZ + MathUtils.floor(fractionZ);

        fractionX = MathUtils.fraction(fractionX);
        fractionY = MathUtils.fraction(fractionY);
        fractionZ = MathUtils.fraction(fractionZ);

        return this;
    }

    public Position addComponent(int component, float value) {

        switch (component) {
            case X_COMPONENT -> {
                fractionX += value;
                longX = longX + MathUtils.floor(fractionX);
                fractionX = MathUtils.fraction(fractionX);
            }
            case Y_COMPONENT -> {
                fractionY += value;
                longY = longY + MathUtils.floor(fractionY);
                fractionY = MathUtils.fraction(fractionY);
            }
            case Z_COMPONENT -> {
                fractionZ += value;
                longZ = longZ + MathUtils.floor(fractionZ);
                fractionZ = MathUtils.fraction(fractionZ);
            }
        }

        return this;
    }

    @Override
    public Vector3f vectorFrom(Distanceable distanceable) {
        if (!(distanceable instanceof Position position)) return new Vector3f(Float.NaN);
        return new Vector3f(
                (longX - position.longX) + (fractionX - position.fractionX),
                (longY - position.longY) + (fractionY - position.fractionY),
                (longZ - position.longZ) + (fractionZ - position.fractionZ)
        );
    }

    public Vector3f getInChunkPosition() {
        return new Vector3f(longX & CHUNK_SIZE_MASK, longY & CHUNK_SIZE_MASK, longZ & CHUNK_SIZE_MASK).add(fractionX, fractionY, fractionZ);
    }

    public Vector3l getChunkCoordinate() {
        return new Vector3l(longX >>> CHUNK_SIZE_BITS, longY >>> CHUNK_SIZE_BITS, longZ >>> CHUNK_SIZE_BITS);
    }

    public Vector3l longPosition() {
        return new Vector3l(longX, longY, longZ);
    }

    public Vector3f fractionPosition() {
        return new Vector3f(fractionX, fractionY, fractionZ);
    }

    public boolean sharesChunkWith(Position position) {
        return longX >>> CHUNK_SIZE_BITS == position.longX >>> CHUNK_SIZE_BITS
                && longY >>> CHUNK_SIZE_BITS == position.longY >>> CHUNK_SIZE_BITS
                && longZ >>> CHUNK_SIZE_BITS == position.longZ >>> CHUNK_SIZE_BITS;
    }

    public String intPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(
                longX,
                longY,
                longZ);
    }

    public String fractionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(MathUtils.round(fractionX, 3), MathUtils.round(fractionY, 3), MathUtils.round(fractionZ, 3));
    }

    public String inChunkPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(longX & CHUNK_SIZE_MASK, longY & CHUNK_SIZE_MASK, longZ & CHUNK_SIZE_MASK);
    }
}
