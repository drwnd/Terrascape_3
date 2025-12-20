package game.server.generation;

import core.assets.Asset;
import game.server.MaterialsData;

import static game.utils.Constants.*;

public final class Structure extends Asset {
    public static final byte MIRROR_X = 1;
    public static final byte MIRROR_Z = 2;
    public static final byte ROTATE_90 = 4;
    public static final byte ALL_TRANSFORMS = MIRROR_X | MIRROR_Z | ROTATE_90;

    public Structure(int sizeX, int sizeY, int sizeZ, MaterialsData materials) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.materials = materials;
    }


    public Structure(byte material) {
        this(16, 16, 16, getMaterialDisplayData(material));
    }

    public byte getMaterial(int structureX, int structureY, int structureZ) {
        if (!contains(structureX, structureY, structureZ)) return AIR;
        return materials.getMaterial(structureX, structureY, structureZ);
    }

    public boolean contains(int structureX, int structureY, int structureZ) {
        return structureX >= 0 && structureX < sizeX
                && structureY >= 0 && structureY < sizeY
                && structureZ >= 0 && structureZ < sizeZ;
    }

    @Override
    public void delete() {

    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public int sizeX(byte transform) {
        return (transform & Structure.ROTATE_90) == 0 ? sizeX : sizeZ;
    }

    public int sizeZ(byte transform) {
        return (transform & Structure.ROTATE_90) == 0 ? sizeZ : sizeX;
    }

    public MaterialsData materials() {
        return materials;
    }

    private static MaterialsData getMaterialDisplayData(byte material) {
        byte[] data = MATERIAL_DISPLAY_DATA_TEMPLATE.clone();
        data[45] = material;
        return new MaterialsData(CHUNK_SIZE_BITS, data);
    }

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final MaterialsData materials;

    private static final byte[] MATERIAL_DISPLAY_DATA_TEMPLATE = new byte[]{2, 0, 0, 60, 0, 0, 62, 0, 0, 64, 0, 0, 66, 0, 0, 68, 0, 0, 70, 0, 0, 72, 2, 0, 0, 24, 0, 0, 26, 0, 0, 28, 0, 0, 30, 0, 0, 32, 0, 0, 34, 0, 0, 36, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
}
