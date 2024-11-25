package lando.systems.game.ui.nodeeditor.objects;

import imgui.extension.nodeditor.NodeEditor;

public class Link2 extends EditorObject {

    public final Pin2 src;
    public final Pin2 dst;

    public Link2(Pin2 src, Pin2 dst) {
        super(EditorObject.Type.LINK);
        this.src = src;
        this.dst = dst;
    }

    public void render() {
        NodeEditor.link(globalId, src.globalId, dst.globalId);
    }

    @Override
    public String toString() {
        return STR."Link\{objectId}#\{globalId}: \{src} -> \{dst}";
    }
}
