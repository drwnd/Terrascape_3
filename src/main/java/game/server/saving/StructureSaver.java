package game.server.saving;

import game.server.MaterialsData;
import game.server.generation.Structure;

public final class StructureSaver extends Saver<Structure> {
    @Override
    void save(Structure structure) {
        saveInt(structure.sizeX());
        saveInt(structure.sizeY());
        saveInt(structure.sizeZ());
        saveInt(structure.materials().getTotalSizeBits());
        saveByteArray(structure.materials().getBytes());
    }

    @Override
    Structure load() {
        int sizeX = loadInt();
        int sizeY = loadInt();
        int sizeZ = loadInt();
        int totalSizeBits = loadInt();
        byte[] data = loadByteArray();

        return new Structure(sizeX, sizeY, sizeZ, new MaterialsData(totalSizeBits, data));
    }

    @Override
    Structure getDefault() {
        return null;
    }

    @Override
    int getVersionNumber() {
        return 0;
    }
}
