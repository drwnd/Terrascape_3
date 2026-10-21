package game.player.particles;

public enum ParticleType {

    OPAQUE_BREAK(40, true),
    OPAQUE_PLACE(10, true),
    TRANSPARENT_BREAK(40, false),
    TRANSPARENT_PLACE(10, false),
    SPLASH(20, true);

    ParticleType(int lifeTimeTicks, boolean isOpaque) {
        this.lifeTimeTicks = lifeTimeTicks;
        this.isOpaque = isOpaque;
    }

    public int getLifeTimeTicks() {
        return lifeTimeTicks;
    }

    public boolean isOpaque() {
        return isOpaque;
    }


    private final int lifeTimeTicks;
    private final boolean isOpaque;
}
