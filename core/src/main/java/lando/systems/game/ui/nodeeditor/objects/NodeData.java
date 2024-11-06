package lando.systems.game.ui.nodeeditor.objects;

import com.badlogic.gdx.utils.GdxRuntimeException;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

public class NodeData<T> {

    private static int nextId = 0;

    public final int id;
    public final Node<T> node;

    public T value;

    public NodeData(Node<T> node, Class<T> clazz) {
        this.id = nextId++;
        this.node = node;
        try {
            if (clazz == Void.class) {
                throw new Exception("Cannot create NodeData with type Void");
            }
            this.value = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new GdxRuntimeException(STR."Failed to construct NodeData value: type \{clazz}", e);
        }
    }

    public void render() {
        switch (value) {
            case ImString text -> {
                ImGui.inputTextMultiline(STR."\{node.pointerId}##text", text,
                    200, 100, ImGuiInputTextFlags.AllowTabInput);
            }
            default -> ImGui.text(STR."Unsupported data type: \{value.getClass().getSimpleName()}");
        }
    }
}
