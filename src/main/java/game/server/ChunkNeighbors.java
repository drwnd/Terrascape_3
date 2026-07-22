package game.server;

import game.utils.Status;

public record ChunkNeighbors(Chunk north, Chunk top, Chunk west, Chunk south, Chunk bottom, Chunk east) {

    /**
     * Checks if any of the neighboring chunks are not yet fully generated.
     * @return true if any neighbor is missing or not in the DONE state, false otherwise
     */
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
