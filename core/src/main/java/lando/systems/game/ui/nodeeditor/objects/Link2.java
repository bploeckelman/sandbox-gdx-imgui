package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImVec4;
import imgui.extension.nodeditor.NodeEditor;

public class Link2 extends EditorObject {

    public final Pin2 src;
    public final Pin2 dst;

    public ImVec4 color = new ImVec4(0f, 1f, 0f, 1f);

    public Link2(Pin2 src, Pin2 dst) {
        super(EditorObject.Type.LINK);
        this.src = src;
        this.dst = dst;
    }

    public void render() {
        NodeEditor.link(globalId, src.globalId, dst.globalId, color, 2f);
    }

    @Override
    public String toString() {
        return STR."Link\{objectId}#\{globalId}: \{src} -> \{dst}";
    }
}
