package game.player.rendering;

public record ParticleEffect(int buffer, long spawnTick, int lifeTimeTicks, int count, boolean isOpaque, int x, int y, int z) {
}

