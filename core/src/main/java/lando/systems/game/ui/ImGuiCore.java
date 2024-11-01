package lando.systems.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import imgui.ImFont;
import imgui.ImGui;
import lando.systems.game.shared.ImGuiPlatform;

public class ImGuiCore implements ImGuiPlatform {

    private final ImGuiPlatform platform;

    private InputProcessor imGuiInputProcessor;

    public ImGuiCore(ImGuiPlatform platform) {
        this.platform = platform;
        this.imGuiInputProcessor = null;
    }

    @Override
    public void init() {
        platform.init();
    }

    @Override
    public void dispose() {
        platform.dispose();
        ImGui.destroyContext();
    }

    @Override
    public void startFrame() {
        // restore the input processor after ImGui caught all inputs
        if (imGuiInputProcessor != null) {
            Gdx.input.setInputProcessor(imGuiInputProcessor);
            imGuiInputProcessor = null;
        }

        platform.startFrame();

        ImGui.newFrame();
    }

    @Override
    public void endFrame() {
        ImGui.render();

        platform.endFrame();

        // if ImGui wants to capture the input, disable libGDX's input processor
        if (ImGui.getIO().getWantCaptureKeyboard() || ImGui.getIO().getWantCaptureMouse()) {
            imGuiInputProcessor = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public ImFont getFont(String name) {
        return platform.getFont(name);
    }
}
