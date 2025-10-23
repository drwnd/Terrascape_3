package game.server.generation;

public record Tree(int centerX, int baseY, int centerZ, Structure structure, byte transform) {

    public int getMinX() {
        return centerX - (sizeX() >> 1);
    }

    public int getMinY() {
        return baseY;
    }

    public int getMinZ() {
        return centerZ - (sizeZ() >> 1);
    }

    public int getMaxY() {
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
