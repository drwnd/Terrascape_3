package game.server.generation;

public record Tree(int centerX, int baseY, int centerZ, Structure structure, byte transform) {

    public int getMinX() {
        return centerX - (structure.sizeX() >> 1);
    }

    public int getMinY() {
        return baseY;
    }

    public int getMinZ() {
        return centerZ - (structure.sizeZ() >> 1);
    }

    public int getMaxY() {
        return baseY + structure.sizeY();
    }
}
