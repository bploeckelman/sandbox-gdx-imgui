package lando.systems.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import lando.systems.game.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class NodeCanvas implements Disposable {

    public enum Type { IM_NODES, NODE_EDITOR }

    protected final ImGuiCore imgui;

    public NodeCanvas(ImGuiCore imgui) {
        this.imgui = imgui;
    }

    public abstract void init();
    public abstract void dispose();
    public abstract void update();
    public abstract void render();

    public void load() {
        var file = Util.openFileDialog();
        if (file != null) {
            try {
                var fileContent = new String(Files.readAllBytes(file.toPath()));
                Gdx.app.log("BlueprintEditor", "Loaded file: " + file.getPath());
                Gdx.app.log("BlueprintEditor", "File content: " + fileContent);
            } catch (IOException e) {
                Gdx.app.error("BlueprintEditor", "Failed to read file", e);
            }
        } else {
            Gdx.app.log("BlueprintEditor", "No file selected");
        }
    }

    public void save() {
        Gdx.app.error(NodeCanvas.class.getSimpleName(), "save() not implemented");
    }
}
