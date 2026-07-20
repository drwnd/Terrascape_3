package game.server.generation;

import core.assets.Asset;
import game.server.materials_data.MaterialsData;

import static game.utils.Constants.*;

public record Structure(int sizeX, int sizeY, int sizeZ, MaterialsData materials) implements Asset {
    public static final byte MIRROR_X = 1;
    public static final byte MIRROR_Z = 2;
    public static final byte ROTATE_90 = 4;
    public static final byte ALL_TRANSFORMS = MIRROR_X | MIRROR_Z | ROTATE_90;


    public Structure(byte material) {
        this(16, 16, 16, new MaterialsData(4, material));
    }

    public Structure(int sizeBits, byte material) {
        this(1 << sizeBits, 1 << sizeBits, 1 << sizeBits, new MaterialsData(sizeBits, material));
    }

/**
 * Creates a new Structure instance.
 *
 * @param sizeBits parameter
 * @param material parameter
 * @param bitMap parameter
 */
    public Structure(int sizeBits, byte material, long[] bitMap) {
        this(1 << sizeBits, 1 << sizeBits, 1 << sizeBits,
                MaterialsData.getCompressedMaterials(sizeBits, bitMap, material));
    }

    public Structure(int sizeX, int sizeY, int sizeZ, int sizeBits, byte material, long[] bitMap) {
        this(sizeX, sizeY, sizeZ, MaterialsData.getCompressedMaterials(sizeBits, bitMap, material));
    }

/**
 * Returns the material.
 *
 * @param structureX X coordinate in local block coordinates
 * @param structureY Y coordinate in local block coordinates
 * @param structureZ Z coordinate in local block coordinates
 * @return result
 */
    public byte getMaterial(int structureX, int structureY, int structureZ) {
        if (!contains(structureX, structureY, structureZ)) return AIR;
        return materials.getMaterial(structureX, structureY, structureZ);
    }

/**
 * Returns the material.
 *
 * @param structureX X coordinate in local block coordinates
 * @param structureY Y coordinate in local block coordinates
 * @param structureZ Z coordinate in local block coordinates
 * @param transform parameter
 * @return result
 */
    public byte getMaterial(int structureX, int structureY, int structureZ, byte transform) {
        if ((transform & MIRROR_X) != 0) structureX = sizeX(transform) - structureX - 1;
        if ((transform & MIRROR_Z) != 0) structureZ = sizeZ(transform) - structureZ - 1;
        if ((transform & ROTATE_90) != 0) {
            int copy = structureZ;
            structureZ = structureX;
            structureX = sizeZ(transform) - copy - 1;
        }

        return getMaterial(structureX, structureY, structureZ);
    }

/**
 * Performs contains.
 *
 * @param structureX X coordinate in local block coordinates
 * @param structureY Y coordinate in local block coordinates
 * @param structureZ Z coordinate in local block coordinates
 * @return true if the condition holds
 */
    public boolean contains(int structureX, int structureY, int structureZ) {
        return structureX >= 0 && structureX < sizeX
                && structureY >= 0 && structureY < sizeY
                && structureZ >= 0 && structureZ < sizeZ;
    }

    @Override
    public void delete() {

    }

    public int sizeX(byte transform) {
        return (transform & Structure.ROTATE_90) == 0 ? sizeX : sizeZ;
    }

    public int sizeY(byte transform) {
        return sizeY;
    }

    public int sizeZ(byte transform) {
        return (transform & Structure.ROTATE_90) == 0 ? sizeZ : sizeX;
    }
}
