package game.player.interaction.placeable_shapes;

import core.rendering_api.shaders.ComputeShader;
import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.FileManager;
import core.utils.MainThread;
import core.utils.Saver;

import game.language.UiMessages;
import game.player.interaction.Rotation24Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;

import java.nio.file.Path;

public final class CustomShape extends ShapePlaceable {

    @MainThread
    public CustomShape(byte material, String shaderCode) {
        super(() -> new ComputeShader(CODE_TEMPLATE + shaderCode, "CUSTOM"), material, Rotation24Way.ROTATION_01);
        this.shaderCode = shaderCode;
        loadSettings();
    }

    public void save(Saver<?> saver) {
        saver.saveByte((byte) 18);
        saver.saveByte(getMaterial());
        saver.saveString(shaderCode);
        saver.saveInt(lengthX.value());
        saver.saveInt(lengthY.value());
        saver.saveInt(lengthZ.value());
    }

    @MainThread
    public static CustomShape load(Saver<?> saver) {
        CustomShape placeable = new CustomShape(saver.loadByte(), saver.loadString());
        placeable.lengthX.setValue(saver.loadInt());
        placeable.lengthY.setValue(saver.loadInt());
        placeable.lengthZ.setValue(saver.loadInt());
        return placeable;
    }


    @Override
    @MainThread
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CustomShape copy = new CustomShape(material, shaderCode);
        copy.lengthX.setValue(lengthX.value());
        copy.lengthY.setValue(lengthY.value());
        copy.lengthZ.setValue(lengthZ.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[] {
                new ShapeSetting(lengthX, UiMessages.LENGTH_X, null),
                new ShapeSetting(lengthY, UiMessages.LENGTH_Y, null),
                new ShapeSetting(lengthZ, UiMessages.LENGTH_Z, null)
        };
    }

    @Override
    public int unrotatedLengthX() {
        return lengthX.value();
    }

    @Override
    public int unrotatedLengthY() {
        return lengthY.value();
    }

    @Override
    public int unrotatedLengthZ() {
        return lengthZ.value();
    }

    @MainThread
    public void setShaderCode(String shaderCode) {
        this.shaderCode = shaderCode;
        setShaderIdentifier(() -> new ComputeShader(CODE_TEMPLATE + shaderCode, "CUSTOM"));
    }

    private String shaderCode;
    private final StandAloneIntSetting lengthX = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneIntSetting lengthY = new StandAloneIntSetting(0, 256, 16);
    private final StandAloneIntSetting lengthZ = new StandAloneIntSetting(0, 256, 16);

    private static final String CODE_TEMPLATE = FileManager.loadFileContents(Path.of("assets", "shaders", "customShapeShaders", "CustomTemplate.comp"));
}
