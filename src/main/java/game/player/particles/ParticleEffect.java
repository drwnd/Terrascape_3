package game.player.particles;

public record ParticleEffect(int buffer, long spawnTick, int lifeTimeTicks, int count, boolean isOpaque, long x, long y, long z) {
}

