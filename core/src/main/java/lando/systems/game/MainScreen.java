package lando.systems.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import imgui.ImGui;

public class MainScreen extends ScreenAdapter {

    Game game;
    SpriteBatch batch;
    UserInterface ui;

    public MainScreen(Game game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.ui = new UserInterface();
    }

    @Override
    public void show() {
        ui.init();
        ui.renderFunc = imGuiGdx -> {
            ui.camera.update();
            batch.setProjectionMatrix(ui.camera.combined);
            ImGui.ShowDemoWindow();
            return null;
        };
    }

    @Override
    public void render(float dt) {
        ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1);
        ui.render();
    }

    @Override
    public void dispose() {
        ui.dispose();
    }
}
