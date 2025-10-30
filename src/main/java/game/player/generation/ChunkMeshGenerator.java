package game.player.generation;

import game.player.rendering.Mesh;
import game.server.Chunk;
import game.server.Game;

import static game.utils.Constants.*;

public final class ChunkMeshGenerator extends MeshGenerator {

    public void generateMesh(Chunk chunk) {
        Game.getPlayer().getMeshCollector().setMeshed(true, chunk.INDEX, chunk.LOD);
        if (chunk.isAir()) {
            Mesh mesh = new Mesh(new int[0], new int[6], new int[0], 0, 0, chunk.X, chunk.Y, chunk.Z, chunk.LOD);
            Game.getPlayer().getMeshCollector().queueMesh(mesh);
            return;
        }
        chunk.generateSurroundingChunks();
        chunk.generateToMeshFacesMaps(toMeshFacesMap, materials, adjacentChunkLayers);

        clear();
        addNorthSouthFaces();
        addTopBottomFaces();
        addWestEastFaces();

        Mesh mesh = loadMesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD);
        Game.getPlayer().getMeshCollector().queueMesh(mesh);
    }


    private void addNorthSouthFaces() {
        System.arraycopy(adjacentChunkLayers[SOUTH], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(materialZ);
            addNorthSouthLayer(NORTH, materialZ, materialsLayer, toMeshFacesMap[NORTH][materialZ]);
            addNorthSouthLayer(SOUTH, materialZ, materialsLayer, toMeshFacesMap[SOUTH][materialZ]);
        }
    }

    private void copyMaterialsNorthSouth(int materialZ) {
        if (materialZ == CHUNK_SIZE) System.arraycopy(adjacentChunkLayers[NORTH], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);
        else {
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS, materialsLayer, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);
        }
    }

    private void addTopBottomFaces() {
        System.arraycopy(adjacentChunkLayers[BOTTOM], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialsTopBottom(materialY);
            addTopBottomLayer(TOP, materialY, materialsLayer, toMeshFacesMap[TOP][materialY]);
            addTopBottomLayer(BOTTOM, materialY, materialsLayer, toMeshFacesMap[BOTTOM][materialY]);
        }
    }

    private void copyMaterialsTopBottom(int materialY) {
        if (materialY == CHUNK_SIZE) System.arraycopy(adjacentChunkLayers[TOP], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);
        else {
            for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++)
                materialsLayer[index] = materials[index << CHUNK_SIZE_BITS | materialY];
        }
    }

    private void addWestEastFaces() {
        System.arraycopy(adjacentChunkLayers[EAST], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialsWestEast(materialX);
            addWestEastLayer(WEST, materialX, materialsLayer, toMeshFacesMap[WEST][materialX]);
            addWestEastLayer(EAST, materialX, materialsLayer, toMeshFacesMap[EAST][materialX]);
        }
    }

    private void copyMaterialsWestEast(int materialX) {
        if (materialX == CHUNK_SIZE) System.arraycopy(adjacentChunkLayers[WEST], 0, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);
        else System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2, materialsLayer, 0, CHUNK_SIZE * CHUNK_SIZE);
    }
}
