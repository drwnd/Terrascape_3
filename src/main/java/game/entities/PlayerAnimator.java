package game.entities;

import core.utils.MainThread;

import game.player.Player;
import game.server.Game;

public final class PlayerAnimator implements Animator {

    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;

    @Override
    @MainThread
    public void applyAnimation(RenderEntity entity, float frameTime) {
        Player player = Game.getPlayer();
        player.getMovement().getState().applyAnimation(entity, player.getCamera(), frameTime);
    }
}
