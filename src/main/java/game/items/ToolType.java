package game.items;

import core.language.Translatable;

public enum ToolType implements Translatable {

    SWORD, SPEAR, KNIFE, SCIMITAR, AXE, PICKAXE, SHOVEL, WARHAMMER;

    @Override
    public String translationFileName() {
        return "toolTypes";
    }
}
