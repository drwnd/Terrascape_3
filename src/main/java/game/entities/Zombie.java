package game.entities;

import core.utils.MainThread;

import game.assets.Models;
import game.utils.Position;

import org.joml.Vector3f;

public class Zombie extends Entity {

    public Zombie(Position position) {
        super(position, new RenderEntity(Models.ZOMBIE));
    }

    public static class ZombieAnimator implements Animator {
        @Override
        @MainThread
        public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime) {
            Animator.super.applyAnimation(entity, rotation, frameTime);
        }
    }
}
