package game.server;

import game.utils.Status;

/**
 * Performs chunk neighbors.
 *
 * @param north parameter
 * @param top parameter
 * @param west parameter
 * @param south parameter
 * @param bottom parameter
 * @param east parameter
 * @return result
 */
public record ChunkNeighbors(Chunk north, Chunk top, Chunk west, Chunk south, Chunk bottom, Chunk east) {

    public boolean areUnGenerated() {
        if (north == null || top == null || west == null || south == null || bottom == null || east == null) return true;
        return north.getGenerationStatus() != Status.DONE
                || top.getGenerationStatus() != Status.DONE
                || west.getGenerationStatus() != Status.DONE
                || south.getGenerationStatus() != Status.DONE
                || bottom.getGenerationStatus() != Status.DONE
                || east.getGenerationStatus() != Status.DONE;
    }
}
