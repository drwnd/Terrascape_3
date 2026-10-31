package game.entities;

import core.assets.AssetManager;
import core.utils.IntArrayList;
import core.utils.MainThread;
import core.utils.Saver;
import core.utils.Vector3l;

import game.assets.EntityType;
import game.entities.model.ModelPart;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.player.particles.ParticleType;
import game.server.Game;
import game.utils.Position;
import game.utils.ServerThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Entity implements Saver.Savable {

    public Entity(Position position, RenderEntity renderEntity, Class<? extends MovementState> movementState, EntityType entityType) {
        this.position = position;
        this.renderEntity = renderEntity;
        this.movement = new Movement(MovementState.load(movementState));
        this.entityType = entityType;
        renderEntity.synchronizeTransforms();
    }

    public Entity(Position position, EntityType entityType, Class<? extends MovementState> movementState) {
        this.position = position;
        this.renderEntity = new RenderEntity(entityType);
        this.movement = new Movement(MovementState.load(movementState));
        this.entityType = entityType;
        renderEntity.synchronizeTransforms();
    }

    public final void save(Saver<?> saver) {
        if (entityType == EntityType.PLAYER) return;
        saver.saveEnum(entityType);
        saver.saveGeneric(position);
        saver.saveVector3f(rotation);
        saver.saveInt(health);
    }

    public static Entity load(Saver<?> saver) {
        EntityType entityType = saver.loadEnum(EntityType.class);
        Position position = saver.loadGeneric(Position::load);
        Vector3f rotation = saver.loadVector3f();
        int health = saver.loadInt();

        Entity entity = entityType.getEntity(position);
        entity.rotation.set(rotation);
        entity.health = health;
        return entity;
    }

    @ServerThread
    public final void kill() {
        playDeathAnimation();
        if (renderEntity.isCustomModel()) AssetManager.deleteOnFrameUpdate(renderEntity.getModelIdentifier());
    }

    private void playDeathAnimation() {
        synchronized (this) {
            Matrix4f[] transforms = renderEntity.transforms();
            ModelPart[] parts = renderEntity.parts();
            if (transforms.length != parts.length) return;

            IntArrayList particles = new IntArrayList(parts.length * 64);
            Vector3f velocity = movement.getVelocity().mul(3);
            for (int index = 0; index < parts.length; index++) parts[index].spawnParticles(particles, transforms[index], velocity);
            Game.getPlayer().getParticleCollector().addParticles(position.longX, position.longY, position.longZ, particles, ParticleType.OPAQUE_BREAK);
        }
    }

    @ServerThread
    public void hit(Vector3f direction, int damage) {
        movement.applyForce(direction.x * 10, Math.max(0, direction.y) * 10 + 3, direction.z * 10);
        health -= damage;
        if (health <= 0) Game.getServer().killEntity(this);
        else if (damage > 0) isHit.set(true);
    }

    @ServerThread
    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, rotation, movement.isGrounded());
        }
    }

    @MainThread
    public void updateFrame(float frameTime, float gameTickFraction) {
        if (health <= 0) return;
        synchronized (this) {
            renderEntity.updateFrame(frameTime, gameTickFraction, rotation);
            Vector3f movementThisTick = movement.getRenderVelocity().mul(gameTickFraction - 1.0F);
            toRenderPosition.set(position).add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
        }
        if (isHit.getAndSet(false)) renderEntity.hit(getPosition());
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
    private final AtomicBoolean isHit = new AtomicBoolean(false);
    private final RenderEntity renderEntity;
    private final Vector3f rotation = new Vector3f();
    private final EntityType entityType;
    private int health = 20;
}
