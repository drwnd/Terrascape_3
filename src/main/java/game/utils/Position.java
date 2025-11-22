package game.utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Position {

    public int intX, intY, intZ;
    public float fractionX, fractionY, fractionZ;

    public Position() {
        this.intX = 0;
        this.intY = 0;
        this.intZ = 0;
        this.fractionX = 0.0F;
        this.fractionY = 0.0F;
        this.fractionZ = 0.0F;
    }

    public Position(int intX, int intY, int intZ, float fractionX, float fractionY, float fractionZ) {
        this.intX = intX & WORLD_SIZE_XZ_MASK;
        this.intY = intY & WORLD_SIZE_Y_MASK;
        this.intZ = intZ & WORLD_SIZE_XZ_MASK;
        this.fractionX = fractionX;
        this.fractionY = fractionY;
        this.fractionZ = fractionZ;
    }

    public Position(Vector3i intPosition, Vector3f fractionPosition) {
        this.intX = intPosition.x & WORLD_SIZE_XZ_MASK;
        this.intY = intPosition.y & WORLD_SIZE_Y_MASK;
        this.intZ = intPosition.z & WORLD_SIZE_XZ_MASK;
        this.fractionX = fractionPosition.x;
        this.fractionY = fractionPosition.y;
        this.fractionZ = fractionPosition.z;
    }

    public Position(Position position) {
        this.intX = position.intX & WORLD_SIZE_XZ_MASK;
        this.intY = position.intY & WORLD_SIZE_Y_MASK;
        this.intZ = position.intZ & WORLD_SIZE_XZ_MASK;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;
    }

    public Position set(Position position) {
        this.intX = position.intX & WORLD_SIZE_XZ_MASK;
        this.intY = position.intY & WORLD_SIZE_Y_MASK;
        this.intZ = position.intZ & WORLD_SIZE_XZ_MASK;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;

        return this;
    }

    public Position add(float x, float y, float z) {
        fractionX += x;
        fractionY += y;
        fractionZ += z;

        intX = intX + Utils.floor(fractionX) & WORLD_SIZE_XZ_MASK;
        intY = intY + Utils.floor(fractionY) & WORLD_SIZE_Y_MASK;
        intZ = intZ + Utils.floor(fractionZ) & WORLD_SIZE_XZ_MASK;

        fractionX = Utils.fraction(fractionX);
        fractionY = Utils.fraction(fractionY);
        fractionZ = Utils.fraction(fractionZ);

        return this;
    }

    public Position addComponent(int component, float value) {

        switch (component) {
            case X_COMPONENT -> {
                fractionX += value;
                intX = intX + Utils.floor(fractionX) & WORLD_SIZE_XZ_MASK;
                fractionX = Utils.fraction(fractionX);
            }
            case Y_COMPONENT -> {
                fractionY += value;
                intY = intY + Utils.floor(fractionY) & WORLD_SIZE_Y_MASK;
                fractionY = Utils.fraction(fractionY);
            }
            case Z_COMPONENT -> {
                fractionZ += value;
                intZ = intZ + Utils.floor(fractionZ) & WORLD_SIZE_XZ_MASK;
                fractionZ = Utils.fraction(fractionZ);
            }
        }

        return this;
    }

    public Vector3f vectorFrom(Position position) {
        return new Vector3f(
                (Utils.getWrappedPosition(intX, position.intX, WORLD_SIZE_XZ) - position.intX) + (fractionX - position.fractionX),
                (Utils.getWrappedPosition(intY, position.intY, WORLD_SIZE_Y) - position.intY) + (fractionY - position.fractionY),
                (Utils.getWrappedPosition(intZ, position.intZ, WORLD_SIZE_XZ) - position.intZ) + (fractionZ - position.fractionZ)
        );
    }

    public Vector3f getInChunkPosition() {
        return new Vector3f(intX & CHUNK_SIZE_MASK, intY & CHUNK_SIZE_MASK, intZ & CHUNK_SIZE_MASK).add(fractionX, fractionY, fractionZ);
    }

    public Vector3i getChunkCoordinate() {
        return new Vector3i(intX >> CHUNK_SIZE_BITS, intY >> CHUNK_SIZE_BITS, intZ >> CHUNK_SIZE_BITS);
    }

    public Vector3i intPosition() {
        return new Vector3i(intX, intY, intZ);
    }

    public Vector3f fractionPosition() {
        return new Vector3f(fractionX, fractionY, fractionZ);
    }

    public boolean sharesChunkWith(Position position) {
        return intX >> CHUNK_SIZE_BITS == position.intX >> CHUNK_SIZE_BITS
                && intY >> CHUNK_SIZE_BITS == position.intY >> CHUNK_SIZE_BITS
                && intZ >> CHUNK_SIZE_BITS == position.intZ >> CHUNK_SIZE_BITS;
    }

    public String intPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(
                Utils.toFakeXZ(intX),
                Utils.toFakeY(intY),
                Utils.toFakeXZ(intZ));
    }

    public String fractionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(Utils.round(fractionX, 3), Utils.round(fractionY, 3), Utils.round(fractionZ, 3));
    }

    public String inChunkPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(intX & CHUNK_SIZE_MASK, intY & CHUNK_SIZE_MASK, intZ & CHUNK_SIZE_MASK);
    }
}
