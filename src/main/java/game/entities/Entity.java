package game.entities;

import core.utils.MainThread;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.server.Game;
import game.utils.Position;
import game.utils.ServerThread;
import org.joml.Vector3f;

import java.util.ArrayList;

public abstract class Entity {

    public static final ArrayList<Entity> ENTITIES = new ArrayList<>();

    @MainThread
    public Entity(Position position, RenderEntity renderEntity, MovementState movementState) {
        this.position = position;
        this.renderEntity = renderEntity;
        this.movement = new Movement(movementState);
        ENTITIES.add(this);
    }

    public void kill() {
        ENTITIES.remove(this);
    }

    @ServerThread
    public void updateGameTick() {
        synchronized (this) {
            position = movement.computeNextGameTickPosition(position, rotation, movement.isGrounded());
        }
    }

    @MainThread
    public void updateFrame(float frameTime) {
        synchronized (this) {
            renderEntity.updateFrame(frameTime, rotation);
        }
    }

    public Position getPosition() {
        return position;
    }

    public Position getToRenderPosition() {
        float fraction = Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0.0F, 1.0F);
        Vector3f movementThisTick = movement.getRenderVelocity().mul(fraction - 1.0F);
        synchronized (this) {
            return new Position(position).add(movementThisTick.x, movementThisTick.y, movementThisTick.z);
        }
    }

    public RenderEntity getRenderEntity() {
        return renderEntity;
    }

    public Movement getMovement() {
        return movement;
    }

    private final RenderEntity renderEntity;
    private final Movement movement;
    private Position position;
    private final Vector3f rotation = new Vector3f();

    public interface EntityBuilder {
        void getEntity(Position position);
    }
}
