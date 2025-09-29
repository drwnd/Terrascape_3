package game.server.generation;

import java.util.Arrays;

import static game.utils.Constants.*;

public record Structure(int sizeX, int sizeY, int sizeZ, byte[] materials) {

    public Structure(int sizeX, int sizeY, int sizeZ, byte[] materials) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.materials = materials;

        if (materials.length != sizeX * sizeY * sizeZ)
            throw new IllegalArgumentException("Materials has wrong dimensions. Should have %s but has %s".formatted(sizeX * sizeY * sizeZ, materials.length));
    }

    public Structure(byte material) {
        this(16, 16, 16, getFilledArray(material));
    }

    public byte getMaterial(int x, int y, int z) {
        if (!contains(x, y, z)) return AIR;
        return materials[index(x, y, z)];
    }

    public boolean contains(int x, int y, int z) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    public void fillMaterialsInto(byte[] materials) {
        for (int x = 0; x < sizeX; x++)
            for (int z = 0; z < sizeZ; z++)
                System.arraycopy(this.materials, index(x, 0, z), materials, x << CHUNK_SIZE_BITS * 2 | z << CHUNK_SIZE_BITS, sizeY);
    }


    private int index(int x, int y, int z) {
        return (x * sizeZ + z) * sizeY + y;
    }

    private static byte[] getFilledArray(byte contents) {
        byte[] array = new byte[4096];
        Arrays.fill(array, contents);
        return array;
    }
}
