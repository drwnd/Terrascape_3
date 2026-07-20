package game.server.saving;

import core.utils.Saver;

import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;

import static game.utils.Constants.*;

public final class StructureSaver extends Saver<Structure> {

    public static String getSaveFileLocation(String structureName) {
        return "assets/structures/%s".formatted(structureName);
    }

    public static String getSaveFileLocation() {
        return "assets/structures";
    }

/**
 * Performs save.
 *
 * @param structure parameter
 */
    @Override
    protected void save(Structure structure) {
        saveInt(structure.sizeX());
        saveInt(structure.sizeY());
        saveInt(structure.sizeZ());
        saveInt(structure.materials().getTotalSizeBits());
        saveByteArray(structure.materials().getBytes());
    }

/**
 * Performs load.
 * @return result
 */
    @Override
    protected Structure load() {
        int sizeX = loadInt();
        int sizeY = loadInt();
        int sizeZ = loadInt();
        int totalSizeBits = loadInt();
        byte[] data = loadByteArray();

        return new Structure(sizeX, sizeY, sizeZ, new MaterialsData(totalSizeBits, data));
    }

/**
 * Performs load old version.
 *
 * @param versionNumber parameter
 * @return result
 */
    @Override
    protected Structure loadOldVersion(int versionNumber) {
        if (versionNumber == 0) {
            Structure structure = load();
            byte[] uncompressedMaterials = new byte[1 << structure.materials().getTotalSizeBits() * 3];
            structure.materials().fillUncompressedMaterialsInto(uncompressedMaterials);
            MaterialsData materialsData = MaterialsData.getCompressedMaterials(structure.materials().getTotalSizeBits(), uncompressedMaterials);
            return new Structure(structure.sizeX(), structure.sizeY(), structure.sizeZ(), materialsData);
        }
        return super.loadOldVersion(versionNumber);
    }

    @Override
    protected Structure getDefault() {
        return new Structure(OUT_OF_WORLD);
    }

    @Override
    protected int getVersionNumber() {
        return 1;
    }
}
