package game.utils;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import org.joml.Vector3f;

import static game.utils.Constants.*;

public final class Position {

    public long longX, longY, longZ;
    public float fractionX, fractionY, fractionZ;


    public static void save(Position position, Saver<?> saver) {
        saver.saveLong(position.longX);
        saver.saveLong(position.longY);
        saver.saveLong(position.longZ);
        saver.saveFloat(position.fractionX);
        saver.saveFloat(position.fractionY);
        saver.saveFloat(position.fractionZ);
    }

    public static Position load(Saver<?> saver) {
        return new Position(saver.loadLong(), saver.loadLong(), saver.loadLong(), saver.loadFloat(), saver.loadFloat(), saver.loadFloat());
    }


    public Position() {
        this.longX = 0;
        this.longY = 0;
        this.longZ = 0;
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

    public Position(Vector3l intPosition, Vector3f fractionPosition) {
        this.longX = intPosition.x;
        this.longY = intPosition.y;
        this.longZ = intPosition.z;
        this.fractionX = fractionPosition.x;
        this.fractionY = fractionPosition.y;
        this.fractionZ = fractionPosition.z;
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

    public Vector3f vectorFrom(Position position) {
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
