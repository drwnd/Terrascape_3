package game.utils;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import org.joml.Vector3f;

import static game.utils.Constants.*;

public final class Position implements Distanceable {

    public long longX, longY, longZ;
    public float fractionX, fractionY, fractionZ;


    /**
     * Saves the position to the provided saver.
     *
     * @param saver the saver to use for persisting the position
     */
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


    /**
     * Constructs a position with all coordinates and fractions set to zero.
     * The origin is the world origin (0, 0, 0) in absolute world block coordinates (LOD 0).
     */
    public Position() {
        this.longX = 0L;
        this.longY = 0L;
        this.longZ = 0L;
        this.fractionX = 0.0F;
        this.fractionY = 0.0F;
        this.fractionZ = 0.0F;
    }

    /**
     * Constructs a position with the specified absolute world block coordinates (LOD 0) and fractions.
     *
     * @param intX      the absolute world x-coordinate in blocks at LOD 0
     * @param intY      the absolute world y-coordinate in blocks at LOD 0
     * @param intZ      the absolute world z-coordinate in blocks at LOD 0
     * @param fractionX the fractional part of the x-coordinate [0, 1) within the block
     * @param fractionY the fractional part of the y-coordinate [0, 1) within the block
     * @param fractionZ the fractional part of the z-coordinate [0, 1) within the block
     */
    public Position(long intX, long intY, long intZ, float fractionX, float fractionY, float fractionZ) {
        this.longX = intX;
        this.longY = intY;
        this.longZ = intZ;
        this.fractionX = fractionX;
        this.fractionY = fractionY;
        this.fractionZ = fractionZ;
    }

    /**
     * Constructs a position from the specified coordinates and fractions.
     *
     * @param longPosition     the absolute world block coordinates (LOD 0)
     * @param fractionPosition the fractional offsets within the block (LOD 0)
     */
    public Position(Vector3l longPosition, Vector3f fractionPosition) {
        this.longX = longPosition == null ? 0L : longPosition.x;
        this.longY = longPosition == null ? 0L : longPosition.y;
        this.longZ = longPosition == null ? 0L : longPosition.z;
        this.fractionX = fractionPosition == null ? 0.0F : fractionPosition.x;
        this.fractionY = fractionPosition == null ? 0.0F : fractionPosition.y;
        this.fractionZ = fractionPosition == null ? 0.0F : fractionPosition.z;
    }

    /**
     * Constructs a new position by copying the coordinates and fractions from another position.
     *
     * @param position the position to copy from
     */
    public Position(Position position) {
        this.longX = position.longX;
        this.longY = position.longY;
        this.longZ = position.longZ;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;
    }

    /**
     * Sets this position's coordinates and fractions to those of the specified position.
     *
     * @param position the position to copy from
     * @return this position instance for chaining
     */
    public Position set(Position position) {
        this.longX = position.longX;
        this.longY = position.longY;
        this.longZ = position.longZ;
        this.fractionX = position.fractionX;
        this.fractionY = position.fractionY;
        this.fractionZ = position.fractionZ;

        return this;
    }

    /**
     * Adds an offset to this position in absolute world block units (LOD 0).
     *
     * @param x the offset to add to the x-coordinate in blocks (LOD 0)
     * @param y the offset to add to the y-coordinate in blocks (LOD 0)
     * @param z the offset to add to the z-coordinate in blocks (LOD 0)
     * @return this position instance for chaining
     */
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

    /**
     * Adds a value to a specific component of this position in absolute world block units (LOD 0).
     *
     * @param component the component index (X_COMPONENT, Y_COMPONENT, or Z_COMPONENT)
     * @param value     the value to add in blocks (LOD 0)
     * @return this position instance for chaining
     */
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

    /**
     * Calculates the vector from another distanceable object to this position.
     * The result is in absolute world block units (LOD 0).
     *
     * @param distanceable the reference distanceable object
     * @return a vector pointing from the distanceable object to this position, or a NaN vector if the input is not a Position
     */
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

    /**
     * Checks if this position is within the same chunk as another position.
     * Chunk coordinates are calculated at LOD 0.
     *
     * @param position the position to compare with
     * @return true if both positions share the same chunk coordinates at LOD 0, false otherwise
     */
    public boolean sharesChunkWith(Position position) {
        return longX >>> CHUNK_SIZE_BITS == position.longX >>> CHUNK_SIZE_BITS
                && longY >>> CHUNK_SIZE_BITS == position.longY >>> CHUNK_SIZE_BITS
                && longZ >>> CHUNK_SIZE_BITS == position.longZ >>> CHUNK_SIZE_BITS;
    }

    /**
     * Returns a string representation of the integer part of the position.
     * Represents absolute world block coordinates (LOD 0).
     *
     * @return a formatted string of the long coordinates (LOD 0)
     */
    public String intPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(
                longX,
                longY,
                longZ);
    }

    /**
     * Returns a string representation of the fractional part of the position.
     * Represents offsets within an absolute world block (LOD 0).
     *
     * @return a formatted string of the fractional coordinates rounded to 3 decimal places
     */
    public String fractionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(MathUtils.round(fractionX, 3), MathUtils.round(fractionY, 3), MathUtils.round(fractionZ, 3));
    }

    /**
     * Returns a string representation of the position within its chunk.
     * These are in-chunk block coordinates (LOD 0) in the range [0, 63].
     *
     * @return a formatted string of the in-chunk block coordinates (LOD 0)
     */
    public String inChunkPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(longX & CHUNK_SIZE_MASK, longY & CHUNK_SIZE_MASK, longZ & CHUNK_SIZE_MASK);
    }
}
