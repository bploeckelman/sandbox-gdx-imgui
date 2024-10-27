package lando.systems.game.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.utils.GdxRuntimeException;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import lando.systems.game.ImGuiPlatform;
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
        return new Lwjgl3Application(new Main(new ImGuiDesktop()), getDefaultConfiguration());
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
        return configuration;
    }

    private static class ImGuiDesktop implements ImGuiPlatform {

        public long window;
        public ImGuiImplGlfw imGuiGlfw;
        public ImGuiImplGl3 imGuiGl3;

        @Override
        public void init() {
            if (Gdx.graphics instanceof Lwjgl3Graphics lwjgl3Graphics) {
                imGuiGlfw = new ImGuiImplGlfw();
                imGuiGl3 = new ImGuiImplGl3();

                window = lwjgl3Graphics.getWindow().getWindowHandle();
                if (window == 0) {
                    throw new GdxRuntimeException("Failed to create the GLFW window");
                }

                ImGui.createContext();
                var io = ImGui.getIO();
                io.setIniFilename(null);
                io.getFonts().addFontDefault();
                io.getFonts().build();

                imGuiGlfw.init(window, true);
                imGuiGl3.init("#version 150");
            } else {
                throw new GdxRuntimeException("This ImGui platform requires Lwjgl3");
            }
        }

        @Override
        public void startFrame() {
            imGuiGl3.newFrame();
            imGuiGlfw.newFrame();
        }

        @Override
        public void endFrame() {
            imGuiGl3.renderDrawData(ImGui.getDrawData());
        }

        @Override
        public void dispose() {
            imGuiGl3.shutdown();
            imGuiGl3 = null;
            imGuiGlfw.shutdown();
            imGuiGlfw = null;
        }
    }
}
