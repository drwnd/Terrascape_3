package game.server.saving;

import core.utils.Saver;

import game.server.MaterialsData;
import game.server.generation.Structure;

public final class StructureSaver extends Saver<Structure> {

    public static String getSaveFileLocation(String structureName) {
        return "assets/structures/%s".formatted(structureName);
    }

    @Override
    protected void save(Structure structure) {
        saveInt(structure.sizeX());
        saveInt(structure.sizeY());
        saveInt(structure.sizeZ());
        saveInt(structure.materials().getTotalSizeBits());
        saveByteArray(structure.materials().getBytes());
    }

    @Override
    protected Structure load() {
        int sizeX = loadInt();
        int sizeY = loadInt();
        int sizeZ = loadInt();
        int totalSizeBits = loadInt();
        byte[] data = loadByteArray();

        return new Structure(sizeX, sizeY, sizeZ, new MaterialsData(totalSizeBits, data));
    }

    @Override
    protected Structure getDefault() {
        return null;
    }

    @Override
    protected int getVersionNumber() {
        return 0;
    }
}
