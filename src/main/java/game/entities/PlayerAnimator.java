package game.entities;

import core.utils.MainThread;

import game.player.Player;
import game.server.Game;
import org.joml.Vector3f;

public final class PlayerAnimator implements Animator {

    @Override
    @MainThread
    public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime) {
        Player player = Game.getPlayer();
        player.getMovement().getState().applyAnimation(entity, player.getCamera(), frameTime);
    }
}
