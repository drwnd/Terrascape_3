package game.server.generation;

import core.assets.Asset;
import game.server.MaterialsData;

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

    public Structure(int sizeBits, byte material, long[] bitMap) {
        this(1 << sizeBits, 1 << sizeBits, 1 << sizeBits,
                MaterialsData.getCompressedMaterials(sizeBits, populateUncompressedMaterials(bitMap, material, sizeBits)));
    }

    public byte getMaterial(long structureX, long structureY, long structureZ) {
        if (!contains((int) structureX, (int) structureY, (int) structureZ)) return AIR;
        return materials.getMaterial((int) structureX, (int) structureY, (int) structureZ);
    }

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

    public int sizeZ(byte transform) {
        return (transform & Structure.ROTATE_90) == 0 ? sizeZ : sizeX;
    }


    private static byte[] populateUncompressedMaterials(long[] bitMap, byte material, int sizeBits) {
        byte[] uncompressedMaterials = new byte[1 << sizeBits * 3];
        for (int bitsIndex = 0; bitsIndex < bitMap.length; bitsIndex++)
            for (int index = (bitsIndex << 6) + Long.numberOfTrailingZeros(bitMap[bitsIndex]),
                 end = Math.min(bitsIndex + 1 << 6, uncompressedMaterials.length); index < end; index++) {
                if ((bitMap[bitsIndex] & 1L << index) == 0) continue;
                uncompressedMaterials[index] = material;
            }
        return uncompressedMaterials;
    }
}
