package lando.systems.game.ui.nodeeditor;

import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import lando.systems.game.ui.nodeeditor.objects.PinType;

public class NodeFactory {

    public static PinDesc flowPin(int nodeEditorPinKind) {
        return new PinDesc(nodeEditorPinKind, PinType.FLOW);
    }

    public static PinDesc stringInPin(String label) {
        return new PinDesc(NodeEditorPinKind.Input, PinType.STRING, label);
    }

    public static PinDesc stringOutPin(String label) {
        return new PinDesc(NodeEditorPinKind.Output, PinType.STRING, label);
    }

    public static NodeDesc displayText() {
        var node = new NodeDesc();

        node.type = "Display Text";

        node.inputs.add(flowPin(NodeEditorPinKind.Input));
        node.outputs.add(flowPin(NodeEditorPinKind.Output));

        node.inputs.add(stringInPin("> text"));

        return node;
    }

    public static NodeDesc text() {
        var node = new NodeDesc();

        node.type = "Text";
        node.props.strings.put("Text", "Hello, World!");

        node.outputs.add(stringOutPin("text >"));

        return node;
    }
}
