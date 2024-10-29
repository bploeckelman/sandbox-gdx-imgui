package lando.systems.game.ui.nodeeditor.panels;

import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImLong;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.Graph;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;

public class EditorPane {

    private final BlueprintEditor canvasNodeEditor;

    public EditorPane(BlueprintEditor canvasNodeEditor) {
        this.canvasNodeEditor = canvasNodeEditor;
    }

    public void render() {
        var graph = canvasNodeEditor.graph;

        int editorWindowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar;
        if (ImGui.begin("Editor", editorWindowFlags)) {
            NodeEditor.begin("Node Editor");
            {
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

                // handle the context menu separate from rendering
                NodeEditor.suspend();
                {
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
                }
                NodeEditor.resume();
            }
            NodeEditor.end();
        }
        ImGui.end(); // "Blueprint Canvas"
    }
}
