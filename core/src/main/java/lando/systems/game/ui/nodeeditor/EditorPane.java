package lando.systems.game.ui.nodeeditor;

import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.type.ImLong;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.Graph;

public class EditorPane {

    final CanvasNodeEditor canvasNodeEditor;

    public EditorPane(CanvasNodeEditor canvasNodeEditor) {
        this.canvasNodeEditor = canvasNodeEditor;
    }

    public void render(float width) {
        var context = canvasNodeEditor.context;
        var graph = canvasNodeEditor.graph;

        NodeEditor.setCurrentEditor(context);
        NodeEditor.begin("Node Editor", width, ImGui.getWindowHeight());

        for (Graph.GraphNode node : graph.nodes.values()) {
            NodeEditor.beginNode(node.nodeId);

            ImGui.text(node.getName());

            NodeEditor.beginPin(node.getInputPinId(), NodeEditorPinKind.Input);
            ImGui.text(STR."\{FontAwesomeIcons.ArrowCircleRight} In");
            NodeEditor.endPin();

            ImGui.sameLine();

            NodeEditor.beginPin(node.getOutputPinId(), NodeEditorPinKind.Output);
            ImGui.text(STR."Out \{FontAwesomeIcons.ArrowCircleRight}");
            NodeEditor.endPin();

            NodeEditor.endNode();
        }

        if (NodeEditor.beginCreate()) {
            var a = new ImLong();
            var b = new ImLong();
            if (NodeEditor.queryNewLink(a, b)) {
                var source = graph.findByOutput(a.get());
                var target = graph.findByInput(b.get());
                if (source != null && target != null && source.outputNodeId != target.nodeId && NodeEditor.acceptNewItem()) {
                    source.outputNodeId = target.nodeId;
                }
            }
        }
        NodeEditor.endCreate();

        int uniqueLinkId = 1;
        for (var node : graph.nodes.values()) {
            if (graph.nodes.containsKey(node.outputNodeId)) {
                NodeEditor.link(uniqueLinkId++, node.getOutputPinId(), graph.nodes.get(node.outputNodeId).getInputPinId());
            }
        }

        NodeEditor.suspend();

        var nodeWithContextMenu = NodeEditor.getNodeWithContextMenu();
        if (nodeWithContextMenu != -1) {
            ImGui.openPopup("node_context");
            ImGui.getStateStorage().setInt(ImGui.getID("delete_node_id"), (int) nodeWithContextMenu);
        }

        if (ImGui.isPopupOpen("node_context")) {
            var targetNode = ImGui.getStateStorage().getInt(ImGui.getID("delete_node_id"));
            if (ImGui.beginPopup("node_context")) {
                if (ImGui.button("Delete " + graph.nodes.get(targetNode).getName())) {
                    graph.nodes.remove(targetNode);
                    ImGui.closeCurrentPopup();
                }
                ImGui.endPopup();
            }
        }

        if (NodeEditor.showBackgroundContextMenu()) {
            ImGui.openPopup("node_editor_context");
        }

        if (ImGui.beginPopup("node_editor_context")) {
            if (ImGui.button("Create New Node")) {
                var node = graph.createGraphNode();
                var canvas = NodeEditor.screenToCanvas(ImGui.getMousePos());
                NodeEditor.setNodePosition(node.nodeId, canvas);
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        NodeEditor.resume();
        NodeEditor.end();
    }
}
