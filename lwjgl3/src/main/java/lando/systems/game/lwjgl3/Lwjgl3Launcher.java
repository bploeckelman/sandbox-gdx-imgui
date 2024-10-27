package lando.systems.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import lando.systems.game.Main;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "sandbox-imgui";

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(WINDOW_TITLE);
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
        return configuration;
    }

//    private static class ImGuiDesktop implements ImGuiPlatform {
//
//        ImGuiGdxImpl imguiImpl;
//        ImGuiGdxInputMultiplexer imguiInputMux;
//
//        @Override
//        public void init() {
//            ImGui.CreateContext();
//            var io = ImGui.GetIO();
//            io.set_ConfigFlags(ImGuiConfigFlags.ImGuiConfigFlags_DockingEnable);
//
//            imguiImpl = new ImGuiGdxImpl();
//            imguiInputMux = new ImGuiGdxInputMultiplexer();
//            Gdx.input.setInputProcessor(imguiInputMux);
//
//            var fonts = ImGui.GetIO().get_Fonts();
//            var fontFile01 = Gdx.files.internal("fonts/Cousine-Regular.ttf");
//            var fontFile02 = Gdx.files.internal("fonts/DroidSans.ttf");
//
//            fonts.AddFontFromMemoryTTF(fontFile01.readBytes(), 16).setName(fontFile01.name());
//            fonts.AddFontFromMemoryTTF(fontFile02.readBytes(), 20).setName(fontFile02.name());
//        }
//
//        @Override
//        public void startFrame() {
//            imguiImpl.newFrame();
//        }
//
//        @Override
//        public void endFrame() {
//            ImGui.Render();
//            imguiImpl.render(ImGui.GetDrawData());
//        }
//
//        @Override
//        public void dispose() {
//            imguiImpl.dispose();
//            ImGui.disposeStatic();
//            ImGui.DestroyContext();
//        }
//    }
}
