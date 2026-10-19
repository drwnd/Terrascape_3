package game.entities;

import core.utils.MainThread;
import game.utils.Position;
import game.utils.ServerThread;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Entity {

    public static final ArrayList<Entity> ENTITIES = new ArrayList<>();

    public Entity(Position position, RenderEntity renderEntity) {
        this.position = position;
        this.renderEntity = renderEntity;
        ENTITIES.add(this);
    }

    public void kill() {
        ENTITIES.remove(this);
    }

    @ServerThread
    public void updateGameTick() {
        rotation.y += 1;
    }

    @MainThread
    public void updateFrame(float frameTime) {
        renderEntity.updateFrame(frameTime, rotation);
    }

    public Position getPosition() {
        return position;
    }

    public RenderEntity getRenderEntity() {
        return renderEntity;
    }

    private final RenderEntity renderEntity;
    private Position position;
    private Vector3f rotation = new Vector3f();
}
