package game.server.generation;

public record WorldGenerationSettings(long seed, BiomeSamplers biomeSampler, int blockSize) {
}
