package core.renderables;

import org.joml.Vector2i;

public interface Clickable {

    boolean clickOn(Vector2i cursorPos, int button, int action);

}
