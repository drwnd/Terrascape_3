package game.entities;

import core.utils.IntArrayList;
import core.utils.MainThread;
import core.utils.Vector3l;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.player.particles.ParticleType;
import game.server.Game;
import game.utils.Position;
import game.utils.ServerThread;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public abstract class Entity {

    public Entity(Position position, RenderEntity renderEntity, MovementState movementState) {
        this.position = position;
        this.renderEntity = renderEntity;
        this.movement = new Movement(movementState);
        renderEntity.synchronizeTransforms();
    }

    public void kill() {
        synchronized (this) {
            Matrix4f[] transforms = renderEntity.transforms();
            Model.ModelPart[] parts = renderEntity.parts();
            if (transforms.length != parts.length) return;

            IntArrayList particles = new IntArrayList(parts.length * 64);
            for (int index = 0; index < parts.length; index++) parts[index].spawnParticles(particles, transforms[index]);
            Game.getPlayer().getParticleCollector().addParticles(position.longX, position.longY, position.longZ, particles, ParticleType.OPAQUE_BREAK);
        }
    }

    @ServerThread
    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, rotation, movement.isGrounded());
        }
    }

    @MainThread
    public void updateFrame(float frameTime, float gameTickFraction) {
        synchronized (this) {
            renderEntity.updateFrame(frameTime, gameTickFraction, rotation);
            Vector3f movementThisTick = movement.getRenderVelocity().mul(gameTickFraction - 1.0F);
            toRenderPosition.set(position).add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
        }
    }

    public Position getPosition() {
        return position;
    }

    @MainThread
    public Position getToRenderPosition() {
        return toRenderPosition;
    }

    public RenderEntity getRenderEntity() {
        return renderEntity;
    }

    public Movement getMovement() {
        return movement;
    }

    public Vector3l getMinCoordinate() {
        synchronized (this) {
            Vector3i hitboxSize = movement.getState().getHitboxSize();
            return new Vector3l(Movement.minX(position, hitboxSize), Movement.minY(position, hitboxSize), Movement.minZ(position, hitboxSize));
        }
    }

    public Vector3l getMaxCoordinate() {
        synchronized (this) {
            Vector3i hitboxSize = movement.getState().getHitboxSize();
            return new Vector3l(Movement.maxX(position, hitboxSize), Movement.maxY(position, hitboxSize), Movement.maxZ(position, hitboxSize));
        }
    }

    protected final Movement movement;
    protected Position position, toRenderPosition = new Position();
    private final RenderEntity renderEntity;
    private final Vector3f rotation = new Vector3f();
}
