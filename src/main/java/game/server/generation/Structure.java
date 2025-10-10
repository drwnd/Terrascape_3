package game.server.generation;

import game.server.MaterialsData;

import static game.utils.Constants.*;

public record Structure(int sizeX, int sizeY, int sizeZ, MaterialsData materials) {

//    public static final byte MIRROR_X = 1;
//    public static final byte MIRROR_Z = 2;
//    public static final byte ROTATE_90 = 4;

    public Structure(byte material) {
        this(16, 16, 16, new MaterialsData(4, material));
    }

    public byte getMaterial(int structureX, int structureY, int structureZ) {
        if (!contains(structureX, structureY, structureZ)) return AIR;
        return materials.getMaterial(structureX, structureY, structureZ);
    }

//    public byte getMaterial(int structureX, int structureY, int structureZ, byte transform) {
//        if ((transform & ROTATE_90) != 0) {
//            int temp = sizeX - structureX - 1;
//            structureX = structureZ;
//            structureZ = temp;
//        }
//        if ((transform & MIRROR_X) != 0) structureX = sizeX - structureX - 1;
//        if ((transform & MIRROR_Z) != 0) structureZ = sizeZ - structureZ - 1;
//
//        return materials[index(structureX, structureY, structureZ)];
//    }

    public boolean contains(int structureX, int structureY, int structureZ) {
        return structureX >= 0 && structureX < sizeX
                && structureY >= 0 && structureY < sizeY
                && structureZ >= 0 && structureZ < sizeZ;
    }
}
