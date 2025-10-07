package game.utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public record Position(Vector3i intPosition, Vector3f fractionPosition) {

    public Position(Position position) {
        this(new Vector3i(position.intPosition), new Vector3f(position.fractionPosition));
    }

    public void set(Position position) {
        intPosition.set(position.intPosition);
        fractionPosition.set(position.fractionPosition);
    }

    public void add(float x, float y, float z) {
        fractionPosition.add(x, y, z);
        intPosition.add(Utils.floor(fractionPosition.x), Utils.floor(fractionPosition.y), Utils.floor(fractionPosition.z));
        fractionPosition.set(Utils.fraction(fractionPosition.x), Utils.fraction(fractionPosition.y), Utils.fraction(fractionPosition.z));
    }

    public void addComponent(int component, float value) {
        fractionPosition.setComponent(component, fractionPosition.get(component) + value);
        intPosition.setComponent(component, intPosition.get(component) + Utils.floor(fractionPosition.get(component)));
        fractionPosition.setComponent(component, Utils.fraction(fractionPosition.get(component)));
    }

    public Vector3f vectorFrom(Position position) {
        return new Vector3f(
                (intPosition.x - position.intPosition.x) + (fractionPosition.x - position.fractionPosition.x),
                (intPosition.y - position.intPosition.y) + (fractionPosition.y - position.fractionPosition.y),
                (intPosition.z - position.intPosition.z) + (fractionPosition.z - position.fractionPosition.z)
        );
    }

    public Vector3f getInChunkPosition() {
        return new Vector3f(intPosition.x & CHUNK_SIZE_MASK, intPosition.y & CHUNK_SIZE_MASK, intPosition.z & CHUNK_SIZE_MASK).add(fractionPosition);
    }

    public Vector3i getChunkCoordinate() {
        return new Vector3i(intPosition.x >> CHUNK_SIZE_BITS, intPosition.y >> CHUNK_SIZE_BITS, intPosition.z >> CHUNK_SIZE_BITS);
    }

    public boolean sharesChunkWith(Position position) {
        return intPosition.x >> CHUNK_SIZE_BITS == position.intPosition.x >> CHUNK_SIZE_BITS
                && intPosition.y >> CHUNK_SIZE_BITS == position.intPosition.y >> CHUNK_SIZE_BITS
                && intPosition.z >> CHUNK_SIZE_BITS == position.intPosition.z >> CHUNK_SIZE_BITS;
    }

    public String intPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(intPosition.x, intPosition.y, intPosition.z);
    }

    public String fractionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(fractionPosition.x, fractionPosition.y, fractionPosition.z);
    }

    public String chunkCoordinateToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(intPosition.x >> CHUNK_SIZE_BITS, intPosition.y >> CHUNK_SIZE_BITS, intPosition.z >> CHUNK_SIZE_BITS);
    }

    public String inChunkPositionToString() {
        return "[X:%s, Y:%s, Z:%s]".formatted(intPosition.x & CHUNK_SIZE_MASK, intPosition.y & CHUNK_SIZE_MASK, intPosition.z & CHUNK_SIZE_MASK);
    }
}
