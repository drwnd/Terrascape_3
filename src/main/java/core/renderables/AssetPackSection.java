package core.renderables;

import core.assets.AssetManager;
import core.assets.CoreTextures;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.KeySetting;
import core.settings.NumberSetting;
import core.settings.OptionSetting;
import core.settings.ToggleSetting;
import core.utils.FileManager;
import core.utils.MainThread;
import core.utils.Message;
import core.utils.StringGetter;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class AssetPackSection extends CoreSettingsRenderable {

    @MainThread
    public AssetPackSection() {
        super();
        ArrayList<String> activePacks = AssetManager.getActiveAssetPackNames();
        activePacks.removeIf(packName -> !Path.of("assetPacks", packName).toFile().exists());
        ArrayList<String> inactivePacks = getInactivePacks(activePacks);

        loadButtons(inactivePacks, activePacks);
    }

    private static ArrayList<String> getInactivePacks(ArrayList<String> activePacks) {
        File[] allPacks = FileManager.getChildren(Path.of("assetPacks"));
        ArrayList<String> inactivePacks = new ArrayList<>(allPacks.length - activePacks.size());
        for (File file : allPacks) {
            if ("Default".equalsIgnoreCase(file.getName())) continue;
            if (!activePacks.contains(file.getName())) inactivePacks.add(file.getName());
        }
        return inactivePacks;
    }

    @MainThread
    private void loadButtons(ArrayList<String> inactivePacks, ArrayList<String> activePacks) {
        for (Renderable renderable : inactivePackElements) removeRenderable(renderable).delete();
        for (Renderable renderable : activePackElements) removeRenderable(renderable).delete();
        inactivePackElements.clear();
        activePackElements.clear();

        for (String pack : inactivePacks) addInactivePack(pack);
        for (String pack : activePacks) addActivePack(pack);

        for (Renderable renderable : inactivePackElements) addRenderable(renderable);
        for (Renderable renderable : activePackElements) addRenderable(renderable);
        setButtonsVerticalPosition();
    }

    @MainThread
    private void setButtonsVerticalPosition() {
        float scroll = input.getScroll();

        for (int index = 0; index < inactivePackElements.size(); index++)
            inactivePackElements.get(index).setOffsetToParent(0.2F, 0.9625F - (index + 1) * 0.1125F + scroll);

        for (int index = 0; index < activePackElements.size(); index++)
            activePackElements.get(index).setOffsetToParent(0.55F, 0.9625F - (index + 1) * 0.1125F + scroll);
        hoverOver(Input.getCursorPos());
    }


    @MainThread
    private void addInactivePack(String packName) {
        inactivePackElements.add(new InactivePackElement(packName));
    }

    @MainThread
    private void addActivePack(String pack) {
        activePackElements.add(new ActivePackElement(pack));
    }

    @MainThread
    private Clickable activatePack(InactivePackElement inactivePackElement) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            ActivePackElement activePackElement = new ActivePackElement(inactivePackElement.packName);
            addRenderable(activePackElement);
            removeRenderable(inactivePackElement);

            activePackElements.add(activePackElement);
            inactivePackElements.remove(inactivePackElement);

            setButtonsVerticalPosition();
            return ButtonResult.SUCCESS;
        };
    }

    @MainThread
    private Clickable deactivatePack(ActivePackElement activePackElement) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            InactivePackElement inactivePackElement = new InactivePackElement(activePackElement.packName);
            addRenderable(inactivePackElement);
            removeRenderable(activePackElement);

            inactivePackElements.add(inactivePackElement);
            activePackElements.remove(activePackElement);

            setButtonsVerticalPosition();
            return ButtonResult.SUCCESS;
        };
    }

    @Override
    @MainThread
    Clickable getApplyChangesButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            ArrayList<String> activePacks = new ArrayList<>(activePackElements.size());
            for (ActivePackElement activePack : activePackElements) activePacks.add(activePack.packName);
            AssetManager.setActiveAssetPackNames(activePacks);

            Window.popRenderable();
            return ButtonResult.SUCCESS;
        };
    }

    @Override
    @MainThread
    Clickable getResetSettingsButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;

            ArrayList<String> activePacks = new ArrayList<>();
            AssetManager.setActiveAssetPackNames(activePacks);
            loadButtons(getInactivePacks(activePacks), activePacks);

            return ButtonResult.SUCCESS;
        };
    }

    @Override
    @MainThread
    Clickable getBackButtonAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return ButtonResult.IGNORE;
            Window.popRenderable();
            return ButtonResult.SUCCESS;
        };
    }

    @Override
    @MainThread
    public void scrollSettingButtons(float scroll) {
        Vector2f offset = new Vector2f(0, scroll);
        for (Renderable renderable : inactivePackElements) renderable.move(offset);
        for (Renderable renderable : activePackElements) renderable.move(offset);
    }

    @Override
    public <T extends Number> void addSlider(NumberSetting<T> setting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addKeySelector(KeySetting setting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addToggle(ToggleSetting setting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addOption(OptionSetting setting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Number> void addSlider(NumberSetting<T> setting, StringGetter settingName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addKeySelector(KeySetting setting, StringGetter settingName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addToggle(ToggleSetting setting, StringGetter settingName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addOption(OptionSetting setting, StringGetter settingName) {
        throw new UnsupportedOperationException();
    }

    @Override
    @MainThread
    protected void resizeSelfTo(int width, int height) {
        for (ActivePackElement activePackElement : activePackElements) {
            float sizeToParentX = 0.5F / activePackElement.getAspectRatio() / Window.getAspectRatio();
            activePackElement.upButton.setSizeToParent(sizeToParentX, 0.5F);
            activePackElement.downButton.setSizeToParent(sizeToParentX, 0.5F);
        }
    }

    @Override
    public float getMaxScroll() {
        return Math.max(inactivePackElements.size(), activePackElements.size()) * 0.1125F - 1 + 0.0875F;
    }

    private final ArrayList<Renderable> inactivePackElements = new ArrayList<>();
    private final ArrayList<ActivePackElement> activePackElements = new ArrayList<>();

    private class ActivePackElement extends Renderable {

        @MainThread
        private ActivePackElement(String packName) {
            super(new Vector2f(0.35F, 0.1F), new Vector2f(0.6F, 0));
            this.packName = packName;

            UiButton deactivateButton = new UiButton(new Vector2f(6 / 7F, 1.0F), new Vector2f(1 / 7F, 0.0F), deactivatePack(this));
            deactivateButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), new Message(packName)));

            Vector2f sizeToParent = new Vector2f(0.5F / getAspectRatio() / Window.getAspectRatio(), 0.5F);
            upButton = new UiButton(sizeToParent, new Vector2f(0.0F, 0.5F), upClickable(this));
            downButton = new UiButton(sizeToParent, new Vector2f(0.0F, 0.0F), downClickable(this));
            upButton.setRimThicknessMultiplier(0.5F);
            downButton.setRimThicknessMultiplier(0.5F);
            upButton.addRenderable(new UiElement(new Vector2f(0.8F), new Vector2f(0.1F), CoreTextures.UP_ARROW));
            downButton.addRenderable(new UiElement(new Vector2f(0.8F), new Vector2f(0.1F), CoreTextures.DOWN_ARROW));

            addRenderable(deactivateButton);
            addRenderable(upButton);
            addRenderable(downButton);

            setDoAutoFocusScaling(false);
            setPlayFocusSound(false);
        }

        @MainThread
        private Clickable upClickable(ActivePackElement activePackElement) {
            return (Vector2i _, int _, int action) -> {
                if (action != GLFW_PRESS) return ButtonResult.IGNORE;

                int index = activePackElements.indexOf(activePackElement);
                if (index == -1) return ButtonResult.FAILURE;
                if (index == 0) return ButtonResult.IGNORE;

                activePackElements.set(index, activePackElements.get(index - 1));
                activePackElements.set(index - 1, activePackElement);

                setButtonsVerticalPosition();
                return ButtonResult.SUCCESS;
            };
        }

        @MainThread
        private Clickable downClickable(ActivePackElement activePackElement) {
            return (Vector2i _, int _, int action) -> {
                if (action != GLFW_PRESS) return ButtonResult.IGNORE;

                int index = activePackElements.indexOf(activePackElement);
                if (index == -1) return ButtonResult.FAILURE;
                if (index == activePackElements.size() - 1) return ButtonResult.IGNORE;

                activePackElements.set(index, activePackElements.get(index + 1));
                activePackElements.set(index + 1, activePackElement);

                setButtonsVerticalPosition();
                return ButtonResult.SUCCESS;
            };
        }


        @Override
        @MainThread
        public void setFocused(boolean focused) {
            Vector2i pixelCoordinate = Input.getCursorPos();
            for (Renderable renderable : getChildren()) renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
        }

        private final String packName;
        private final UiButton upButton, downButton;
    }

    private class InactivePackElement extends UiButton {

        @MainThread
        private InactivePackElement(String packName) {
            super(new Vector2f(0.3F, 0.1F), new Vector2f(0.2F, 0));
            this.packName = packName;

            addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), new Message(packName)));
            setAction(activatePack(this));
        }

        private final String packName;
    }
}
