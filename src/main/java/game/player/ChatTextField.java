package game.player;

import core.languages.UiMessage;
import core.renderables.TextField;
import core.rendering_api.Input;
import org.joml.Vector2f;

public final class ChatTextField extends TextField {

    public ChatTextField() {
        super(new Vector2f(1.0F, 0.05F), new Vector2f(0.0F, 0.0F), UiMessage.CHAT_MESSAGE_PROMPT);
        setVisible(false);
        setScaleWithGuiSize(false);
        setAllowFocusScaling(false);
        setCenterText(false);
        setRimThicknessMultiplier(0.5F);
    }

    public Input getInput() {
        return input;
    }

    ChatInput input = new ChatInput(this);
}
