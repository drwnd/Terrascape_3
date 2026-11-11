package game.items;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.language.Translatable;
import core.utils.FileManager;

import java.nio.file.Path;

public enum ToolType implements Translatable {

    SWORD, SPEAR, KNIFE, SCIMITAR, AXE, PICKAXE, SHOVEL, WARHAMMER;

    static {
        AssetManager.addDeleteAllCallback(() -> {
            for (ToolType type : values()) type.load();
        });
    }

    ToolType() {
        load();
    }

    public float getHeldOffset() {
        return heldOffset;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackTime() {
        return attackTime;
    }

    public int getReach() {
        return reach;
    }

    @Override
    public String translationFileName() {
        return "toolTypes";
    }

    private void load() {
        Path assetFilepath = AssetManager.getAssetFilepath(Path.of("items", "toolTypes", name() + ".json"));
        ToolTypeData data = new Gson().fromJson(FileManager.loadJson(assetFilepath), ToolTypeData.class);
        this.heldOffset = data.heldOffset;
        this.damage = data.damage;
        this.attackTime = data.attackTime;
        this.reach = data.reach;
    }

    private float heldOffset;
    private int damage, attackTime, reach;

    private record ToolTypeData(float heldOffset, int damage,int attackTime, int reach) {
    }
}
