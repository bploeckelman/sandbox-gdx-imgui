package lando.systems.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.ScreenAdapter;
import imgui.ImGuiLoader;

public class LoadScreen extends ScreenAdapter {

    Game game;
    boolean initialized = false;

    public LoadScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        ImGuiLoader.init(() -> initialized = true);
    }

    @Override
    public void render(float dt) {
        if (initialized) {
            initialized = false;
            game.setScreen(new MainScreen(game));
        }
    }
}
