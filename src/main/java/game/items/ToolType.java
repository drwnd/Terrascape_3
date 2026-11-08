package game.items;

import core.language.Translatable;

public enum ToolType implements Translatable {

    SWORD(3),
    SPEAR(15),
    KNIFE(3),
    SCIMITAR(3),
    AXE(6),
    PICKAXE(6),
    SHOVEL(6),
    WARHAMMER(12);

    ToolType(float heldOffset) {
        this.heldOffset = heldOffset;
    }

    public float getHeldOffset() {
        return heldOffset;
    }

    @Override
    public String translationFileName() {
        return "toolTypes";
    }

    private final float heldOffset;
}
