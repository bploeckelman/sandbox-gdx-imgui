package lando.systems.game;

import com.badlogic.gdx.InputMultiplexer;

public interface ImGuiRenderer {
    void init();
    void begin();
    void render();
    void end();
    void dispose();
    InputMultiplexer getInputMux();
}
