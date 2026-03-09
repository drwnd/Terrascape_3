package game.player;

import core.language.CoreUiMessages;
import core.renderables.TextField;
import org.joml.Vector2f;

public final class ChatTextField extends TextField {

    public ChatTextField() {
        super(new Vector2f(1.0F, 0.05F), new Vector2f(0.0F, 0.0F), CoreUiMessages.CHAT_MESSAGE_PROMPT);
        setVisible(false);
        setScaleWithGuiSize(false);
        setAllowFocusScaling(false);
        setCenterText(false);
        setRimThicknessMultiplier(0.5F);
    }

    public ChatInput getInput() {
        return input;
    }

    ChatInput input = new ChatInput(this);
}
