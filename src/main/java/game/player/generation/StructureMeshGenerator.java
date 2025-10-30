package game.player.generation;

import game.player.rendering.Mesh;
import game.server.generation.Structure;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class StructureMeshGenerator extends MeshGenerator {

    public ArrayList<Mesh> generateMesh(Structure structure) {
        ArrayList<Mesh> meshes = new ArrayList<>();

        int endX = structure.sizeX();
        int endY = structure.sizeY();
        int endZ = structure.sizeZ();

        for (int structureX = 0; structureX < endX; structureX += CHUNK_SIZE)
            for (int structureY = 0; structureY < endY; structureY += CHUNK_SIZE)
                for (int structureZ = 0; structureZ < endZ; structureZ += CHUNK_SIZE) {
                    int chunkX = structureX >> CHUNK_SIZE_BITS;
                    int chunkY = structureY >> CHUNK_SIZE_BITS;
                    int chunkZ = structureZ >> CHUNK_SIZE_BITS;
                    clear();
                    structure.materials().fillUncompressedMaterialsInto(materials, CHUNK_SIZE_BITS,
                            0, 0, 0,
                            structureX, structureY, structureZ,
                            CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);
                    structure.materials().generateToMeshFacesMaps(toMeshFacesMap, materials, adjacentChunkLayers, chunkX, chunkY, chunkZ);

                    addNorthSouthFaces(structure, structureX, structureY, structureZ);
                    addTopBottomFaces(structure, structureX, structureY, structureZ);
                    addWestEastFaces(structure, structureX, structureY, structureZ);
                    meshes.add(loadMesh(chunkX, chunkY, chunkZ, 0));
                }
        return meshes;
    }

    private void addNorthSouthFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsNorthSouth(structure, structureX, structureY, structureZ, 0, materialsLayer);

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(structure, structureX, structureY, structureZ, materialZ, materialsLayer);
            addNorthSouthLayer(NORTH, materialZ, materialsLayer, toMeshFacesMap[NORTH][materialZ]);
            addNorthSouthLayer(SOUTH, materialZ, materialsLayer, toMeshFacesMap[SOUTH][materialZ]);
        }
    }

    private void copyMaterialsNorthSouth(Structure structure, int structureX, int structureY, int structureZ, int materialZ, byte[] target) {
        if (materialZ < CHUNK_SIZE) {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS, target, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);
        } else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    target[materialX << CHUNK_SIZE_BITS | materialY] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }

    private void addTopBottomFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsTopBottom(structure, structureX, structureY, structureZ, 0, materialsLayer);

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(structure, structureX, structureY, structureZ, materialY, materialsLayer);
            addTopBottomLayer(TOP, materialY, materialsLayer, toMeshFacesMap[TOP][materialY]);
            addTopBottomLayer(BOTTOM, materialY, materialsLayer, toMeshFacesMap[BOTTOM][materialY]);
        }
    }

    private void copyMaterialsTopBottom(Structure structure, int structureX, int structureY, int structureZ, int materialY, byte[] target) {
        if (materialY < CHUNK_SIZE) {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    target[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS | materialY];

        } else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    target[materialX << CHUNK_SIZE_BITS | materialZ] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }

    private void addWestEastFaces(Structure structure, int structureX, int structureY, int structureZ) {
        copyMaterialsWestEast(structure, structureX, structureY, structureZ, 0, materialsLayer);

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(structure, structureX, structureY, structureZ, materialX, materialsLayer);
            addWestEastLayer(WEST, materialX, materialsLayer, toMeshFacesMap[WEST][materialX]);
            addWestEastLayer(EAST, materialX, materialsLayer, toMeshFacesMap[EAST][materialX]);
        }
    }

    private void copyMaterialsWestEast(Structure structure, int structureX, int structureY, int structureZ, int materialX, byte[] target) {
        if (materialX < CHUNK_SIZE) {
            System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2, target, 0, CHUNK_SIZE * CHUNK_SIZE);
        } else {
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    target[materialZ << CHUNK_SIZE_BITS | materialY] = structure.getMaterial(structureX + materialX, structureY + materialY, structureZ + materialZ);
        }
    }
}
