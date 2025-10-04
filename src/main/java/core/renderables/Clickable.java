package core.renderables;

import org.joml.Vector2i;

public interface Clickable {

    void clickOn(Vector2i cursorPos, int button, int action);

}
