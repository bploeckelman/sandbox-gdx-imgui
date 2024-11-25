package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import lando.systems.game.ui.nodeeditor.PinDesc;

public class Pin2 extends EditorObject {

    public final Node2 node;
    public final int kind;
    public final PinType type;
    public final String label;

    public Pin2(Node2 node, PinDesc desc) {
        super(EditorObject.Type.PIN);
        this.node = node;
        this.kind = desc.kind;
        this.type = desc.type;
        this.label = desc.label;
    }

    public void render() {
        NodeEditor.beginPin(globalId, kind);
        ImGui.pushID(globalId);
        ImGui.beginGroup();

        ImGui.text(label);

        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endPin();
    }

    public Link2 connectTo(Pin2 other) {
        return new Link2(this, other);
    }

    @Override
    public String toString() {
        return STR."Pin\{objectId}#\{globalId}: '\{label}' (\{type}, \{kindStr()})";
    }

    public String kindStr() {
        return switch (kind) {
            case NodeEditorPinKind.Input -> "input";
            case NodeEditorPinKind.Output -> "output";
            default -> "unknown";
        };
    }
}
