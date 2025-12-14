package game.player.rendering;


import game.server.Server;

import static game.player.rendering.ParticleCollector.*;

public record ParticleEffect(int buffer, int spawnTime, int lifeTimeTicks, int count, boolean isOpaque, int x, int y, int z) {

    int getLifeTimeNanoSecondsShifted() {
        return lifeTimeTicks * (Server.NANOSECONDS_PER_SECOND / Server.TARGET_TPS >> PARTICLE_TIME_SHIFT);
    }

}

