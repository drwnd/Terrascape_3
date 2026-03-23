package game.player.rendering;

import core.renderables.TextElement;
import core.renderables.UiBackgroundElement;
import core.rendering_api.shaders.TextShader;
import core.settings.CoreFloatSettings;
import core.settings.CoreOptionSettings;
import core.settings.optionSettings.FontOption;

import game.language.UiMessages;
import game.player.Hotbar;
import game.settings.FloatSettings;
import game.settings.IntSettings;

import org.joml.Vector2f;

public final class BreakPlaceOptionsDisplay extends UiBackgroundElement {

    public BreakPlaceOptionsDisplay() {
        super(new Vector2f(), new Vector2f());
        setRimThicknessMultiplier(0.2F);
        setScaleWithGuiSize(false);

        breakPlaceSize.setScaleWithGuiSize(false);
        breakPlaceAlign.setScaleWithGuiSize(false);
        addRenderable(breakPlaceSize);
        addRenderable(breakPlaceAlign);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        breakPlaceSize.setText(UiMessages.BREAK_PLACE_SIZE_FORMAT.get().formatted(1 << IntSettings.BREAK_PLACE_SIZE.value()));
        breakPlaceAlign.setText(UiMessages.BREAK_PLACE_ALIGN_FORMAT.get().formatted(1 << IntSettings.BREAK_PLACE_ALIGN.value()));

        float hotbarSize = FloatSettings.HOTBAR_SIZE.value();
        Vector2f defaultTextSize = ((FontOption) CoreOptionSettings.FONT.value()).getDefaultTextSize();
        float charWidth = defaultTextSize.x;
        float charHeight = defaultTextSize.y * CoreFloatSettings.TEXT_SIZE.value();
        float sizeLength = TextShader.getTextLength(breakPlaceSize.getText(), charWidth, false);
        float alignLength = TextShader.getTextLength(breakPlaceAlign.getText(), charWidth, false);

        setOffsetToParent(0.5F + hotbarSize * Hotbar.LENGTH * 0.5F, 0);
        setSizeToParent(Math.max(sizeLength, alignLength) * 1.1F, charHeight * 2.5F);
        super.renderSelf(getPosition(), getSize());
    }

    TextElement breakPlaceSize = new TextElement(new Vector2f(0.05F, 2.0F / 3.0F));
    TextElement breakPlaceAlign = new TextElement(new Vector2f(0.05F, 1.0F / 3.0F));
}
