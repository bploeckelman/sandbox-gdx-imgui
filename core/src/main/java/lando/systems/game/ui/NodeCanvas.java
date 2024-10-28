package lando.systems.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;

public abstract class NodeCanvas implements Disposable {

    public enum Type { IM_NODES, NODE_EDITOR }

    protected final ImGuiCore imgui;

    public NodeCanvas(ImGuiCore imgui) {
        this.imgui = imgui;
    }

    public abstract void init();
    public abstract void dispose();
    public abstract void render();

    public void load() {
        Gdx.app.error(NodeCanvas.class.getSimpleName(), "load() not implemented");
    }

    public void save() {
        Gdx.app.error(NodeCanvas.class.getSimpleName(), "save() not implemented");
    }
}
