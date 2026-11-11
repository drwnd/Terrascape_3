package game.items;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.language.Translatable;
import core.utils.FileManager;

import java.nio.file.Path;

public enum ToolGrade implements Translatable {

    WOOD, STONE, IRON, DIAMOND;

    static {
        AssetManager.addDeleteAllCallback(() -> {
            for (ToolGrade grade : values()) grade.load();
        });
    }

    ToolGrade() {
        load();
    }

    @Override
    public String translationFileName() {
        return "toolGrades";
    }

    public int getDamage() {
        return damage;
    }

    private void load() {
        Path assetFilepath = AssetManager.getAssetFilepath(Path.of("items", "toolGrades", name() + ".json"));
        ToolGradeData data = new Gson().fromJson(FileManager.loadJson(assetFilepath), ToolGradeData.class);
        this.damage = data.damage;
    }

    private int damage;

    private record ToolGradeData(int damage) {
    }
}
