package core.renderables;

import core.utils.MainThread;
import org.joml.Vector2i;

public interface Clickable {

    @MainThread
    ButtonResult clickOn(Vector2i cursorPos, int button, int action);

}
