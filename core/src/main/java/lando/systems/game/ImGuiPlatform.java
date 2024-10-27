package lando.systems.game;

public interface ImGuiPlatform {

    void init();
    void startFrame();
    void endFrame();
    void dispose();

}
