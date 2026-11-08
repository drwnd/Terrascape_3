package game.items;

import core.language.Translatable;

public enum ToolGrade implements Translatable {

    WOOD, STONE, IRON, DIAMOND;

    @Override
    public String translationFileName() {
        return "toolGrades";
    }
}
