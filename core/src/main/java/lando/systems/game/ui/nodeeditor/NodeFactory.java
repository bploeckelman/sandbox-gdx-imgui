package lando.systems.game.ui.nodeeditor;

import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import lando.systems.game.ui.nodeeditor.objects.PinType;

public class NodeFactory {

    public static PinDesc flowPin(int nodeEditorPinKind) {
        return new PinDesc(nodeEditorPinKind, PinType.FLOW);
    }

    public static PinDesc stringPin(String label) {
        return new PinDesc(NodeEditorPinKind.Input, PinType.STRING, label);
    }

    public static NodeDesc printText() {
        var node = new NodeDesc();

        node.type = "Print Text";

        node.inputs.add(flowPin(NodeEditorPinKind.Input));
        node.outputs.add(flowPin(NodeEditorPinKind.Output));

        node.inputs.add(stringPin("-> Text"));

        return node;
    }
}
