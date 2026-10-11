package game.server.generation;

import core.assets.Asset;
import game.server.material.Properties;
import game.server.materials_data.MaterialsData;

import static game.utils.Constants.*;

public record Structure(int sizeX, int sizeY, int sizeZ, int centerX, int centerY, int centerZ, MaterialsData materials) implements Asset {
    public static final byte MIRROR_X = 1;
    public static final byte MIRROR_Z = 2;
    public static final byte ROTATE_90 = 4;
    public static final byte ALL_TRANSFORMS = MIRROR_X | MIRROR_Z | ROTATE_90;

    public static final MaterialsData.OverrideFunction FORCE_OVERRIDE = (_, material) -> material;
    public static final MaterialsData.OverrideFunction WORLD_GENERATION_OVERRIDE = ((previousMaterial, material) ->
            material != AIR && (Properties.hasProperties(previousMaterial, STRUCTURE_REPLACEABLE) || Properties.doesntHaveProperties(material, STRUCTURE_REPLACEABLE))
                    ? material : previousMaterial);


    public Structure(byte material) {
        this(16, 16, 16, new MaterialsData(4, material));
    }

    public Structure(int sizeBits, byte material) {
        this(1 << sizeBits, 1 << sizeBits, 1 << sizeBits, new MaterialsData(sizeBits, material));
    }

    public Structure(int sizeBits, byte material, long[] bitMap) {
        this(1 << sizeBits, 1 << sizeBits, 1 << sizeBits,
                MaterialsData.getCompressedMaterials(sizeBits, bitMap, material));
    }

    public Structure(int sizeX, int sizeY, int sizeZ, int centerX, int centerY, int centerZ, int sizeBits, byte material, long[] bitMap) {
        this(sizeX, sizeY, sizeZ, centerX, centerY, centerZ, MaterialsData.getCompressedMaterials(sizeBits, bitMap, material));
    }

    public Structure(int sizeX, int sizeY, int sizeZ, MaterialsData materials) {
        this(sizeX, sizeY, sizeZ, sizeX >> 1, 0, sizeZ >> 1, materials);
    }

    public byte getMaterial(int structureX, int structureY, int structureZ) {
        if (!contains(structureX, structureY, structureZ)) return AIR;
        return materials.getMaterial(structureX, structureY, structureZ);
    }

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

    public boolean contains(int structureX, int structureY, int structureZ) {
        return structureX >= 0 && structureX < sizeX
                && structureY >= 0 && structureY < sizeY
                && structureZ >= 0 && structureZ < sizeZ;
    }

    @Override
    public void delete() {

    }

    public int sizeX(byte transform) {
        return (transform & ROTATE_90) == 0 ? sizeX : sizeZ;
    }

    public int sizeY(byte transform) {
        return sizeY;
    }

    public int sizeZ(byte transform) {
        return (transform & ROTATE_90) == 0 ? sizeZ : sizeX;
    }

    public int centerX(byte transform) {
        int centerX = (transform & ROTATE_90) == 0 ? this.centerX : centerZ;
        return (transform & MIRROR_X) == 0 ? centerX : sizeX(transform) - centerX;
    }

    public int centerY(byte transform) {
        return centerY;
    }

    public int centerZ(byte transform) {
        int centerZ = (transform & ROTATE_90) == 0 ? this.centerZ : sizeZ(transform) - centerX;
        return (transform & MIRROR_Z) == 0 ? centerZ : sizeZ(transform) - centerZ;
    }
}
