package game.player.rendering;

import core.renderables.TextElement;
import core.renderables.UiBackgroundElement;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.OptionSetting;
import core.settings.optionSettings.FontOption;

import game.player.Hotbar;
import game.server.Game;

import org.joml.Vector2f;

public final class BreakPlaceOptionsDisplay extends UiBackgroundElement {

    public BreakPlaceOptionsDisplay() {
        super(new Vector2f(), new Vector2f());
        setRimThicknessMultiplier(0.2f);
        setScaleWithGuiSize(false);

        breakPlaceSize.setScaleWithGuiSize(false);
        addRenderable(breakPlaceSize);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        breakPlaceSize.setText("%s³ Voxel ".formatted(1 << Game.getPlayer().getInteractionHandler().getPlaceBreakSize()));

        float hotbarSize = FloatSetting.HOTBAR_SIZE.value();
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        float charWidth = defaultTextSize.x;
        float charHeight = defaultTextSize.y * FloatSetting.TEXT_SIZE.value();
        float textLength = TextShader.getTextLength(breakPlaceSize.getText(), charWidth, scalesWithGuiSize());

        setOffsetToParent(0.5f + hotbarSize * Hotbar.LENGTH * 0.5f, 0);
        setSizeToParent(textLength, charHeight * 1.25f);
        super.renderSelf(getPosition(), getSize());
    }

    TextElement breakPlaceSize = new TextElement(new Vector2f(0.05f, 0.5f));
}
