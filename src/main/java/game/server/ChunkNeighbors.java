package game.server;

import game.utils.Status;

public record ChunkNeighbors(Chunk center, Chunk north, Chunk top, Chunk west, Chunk south, Chunk bottom, Chunk east) {

    public boolean areUnGenerated() {
        if (center == null || north == null || top == null || west == null || south == null || bottom == null || east == null) return true;
        return center.getGenerationStatus() != Status.DONE
                || north.getGenerationStatus() != Status.DONE
                || top.getGenerationStatus() != Status.DONE
                || west.getGenerationStatus() != Status.DONE
                || south.getGenerationStatus() != Status.DONE
                || bottom.getGenerationStatus() != Status.DONE
                || east.getGenerationStatus() != Status.DONE;
    }
}
