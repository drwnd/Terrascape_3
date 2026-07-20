package game.server.generation;

/**
 * Performs tree.
 *
 * @param centerX X coordinate in local block coordinates
 * @param baseY Y coordinate in local block coordinates
 * @param centerZ Z coordinate in local block coordinates
 * @param structure parameter
 * @param transform parameter
 * @return result
 */
public record Tree(long centerX, long baseY, long centerZ, Structure structure, byte transform) {

    public long getMinX() {
        return centerX - (sizeX() >> 1);
    }

    public long getMinY() {
        return baseY;
    }

    public long getMinZ() {
        return centerZ - (sizeZ() >> 1);
    }

    public long getMaxY() {
        return baseY + structure.sizeY();
    }

    public int sizeX() {
        return structure.sizeX(transform);
    }

    public int sizeY() {
        return structure.sizeY();
    }

    public int sizeZ() {
        return structure.sizeZ(transform);
    }
}
