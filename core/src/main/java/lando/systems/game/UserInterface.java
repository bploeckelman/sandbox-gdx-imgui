package lando.systems.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import imgui.ImGui;
import imgui.gdx.ImGuiGdxImpl;
import imgui.gdx.ImGuiGdxInputMultiplexer;

import java.util.function.Function;

import static imgui.ImGuiConfigFlags.*;

public class UserInterface implements ImGuiRenderer {

    public static boolean AUTO_BEGIN_END = true;

    ImGuiGdxImpl imgui;
    ImGuiGdxInputMultiplexer imguiMux;

    OrthographicCamera camera;
    Function<ImGuiGdxImpl, Void> renderFunc;

    public UserInterface() {
        this(null);
    }

    public UserInterface(Function<ImGuiGdxImpl, Void> renderFunc) {
        this.renderFunc = renderFunc;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
        this.camera.update();
    }

    @Override
    public void init() {
        ImGui.CreateContext();
        ImGui.GetIO().set_ConfigFlags(ImGuiConfigFlags_DockingEnable);

        imgui = new ImGuiGdxImpl();
        imguiMux = new ImGuiGdxInputMultiplexer();
        Gdx.input.setInputProcessor(imguiMux);

        var fonts = ImGui.GetIO().get_Fonts();
        var fontFile1 = Gdx.files.internal("fonts/Cousine-Regular.ttf");
        var fontFile2 = Gdx.files.internal("fonts/DroidSans.ttf");
        fonts.AddFontFromMemoryTTF(fontFile1.readBytes(), 16).setName(fontFile1.name());
        fonts.AddFontFromMemoryTTF(fontFile2.readBytes(), 20).setName(fontFile2.name());
    }

    @Override
    public void begin() {
        imgui.newFrame();
    }

    @Override
    public void render() {
        if (AUTO_BEGIN_END) {
            begin();
        }
        if (renderFunc != null) {
            renderFunc.apply(imgui);
        }
        if (AUTO_BEGIN_END) {
            end();
        }
    }

    @Override
    public void end() {
        ImGui.Render();
        imgui.render(ImGui.GetDrawData());
    }

    @Override
    public void dispose() {
        imgui.dispose();
        ImGui.disposeStatic();
        ImGui.DestroyContext();
    }

    @Override
    public InputMultiplexer getInputMux() {
        return imguiMux;
    }
}
