package lando.systems.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import imgui.ImColor;
import imgui.ImGui;
import imgui.extension.imnodes.ImNodes;
import imgui.extension.imnodes.ImNodesEditorContext;
import imgui.extension.imnodes.flag.ImNodesCol;
import imgui.extension.imnodes.flag.ImNodesMiniMapLocation;
import imgui.extension.imnodes.flag.ImNodesPinShape;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImInt;

public class NodeCanvas implements Disposable {

    ImNodesEditorContext context;

    Graph graph;
    ImInt linkA;
    ImInt linkB;

    static class NodeColor {
        static final int title = ImColor.rgb("#2e5266");
        static final int titleHovered = ImColor.rgb("#d3d0cb");
        static final int titleSelected = ImColor.rgb("#e2c044");
    }

    public NodeCanvas() {
        graph = new Graph();
        linkA = new ImInt();
        linkB = new ImInt();
    }

    public void init() {
        ImNodes.createContext();
        ImNodes.styleColorsDark();
        context = ImNodes.editorContextCreate();
    }

    @Override
    public void dispose() {
        ImNodes.editorContextFree(context);
        ImNodes.destroyContext();
    }

    private void openUrl(String url) {
        try {
            // Use libGDX's built-in browser opener
            Gdx.net.openURI(url);
        } catch (Exception e) {
            // Fallback method using Java's desktop integration
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                        desktop.browse(new java.net.URI(url));
                    }
                }
            } catch (Exception ex) {
                Gdx.app.error(NodeCanvas.class.getSimpleName(), "Failed to open URL: " + ex.getMessage());
            }
        }
    }

    public void render() {
        if (ImGui.begin("ImNodes Demo")) {
            ImGui.text("This a demo graph editor for ImNodes");

            ImGui.alignTextToFramePadding();
            ImGui.text("Repo:");
            ImGui.sameLine();
            var URL = "https://github.com/bploeckelman/sandbox-gdx-imgui";
            if (ImGui.button(URL)) {
                openUrl(URL);
            }

            ImNodes.editorContextSet(context);
            ImNodes.beginNodeEditor();

            for (var node : graph.nodes.values()) {
                ImNodes.pushColorStyle(ImNodesCol.TitleBar, NodeColor.title);
                ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, NodeColor.titleHovered);
                ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, NodeColor.titleSelected);

                ImNodes.beginNode(node.nodeId);

                ImNodes.beginNodeTitleBar();
                ImGui.text(node.getName());
                ImNodes.endNodeTitleBar();

                ImNodes.beginInputAttribute(node.getInputPinId(), ImNodesPinShape.CircleFilled);
                ImGui.text("In");
                ImNodes.endInputAttribute();

                ImGui.sameLine();

                ImNodes.beginOutputAttribute(node.getOutputPinId());
                ImGui.text("Out");
                ImNodes.endOutputAttribute();

                ImNodes.endNode();

                ImNodes.popColorStyle();
                ImNodes.popColorStyle();
                ImNodes.popColorStyle();
            }

            int uniqueLinkId = 1;
            for (var node : graph.nodes.values()) {
                if (graph.nodes.containsKey(node.outputNodeId)) {
                    ImNodes.link(uniqueLinkId++, node.getOutputPinId(), graph.nodes.get(node.outputNodeId).getInputPinId());
                }
            }

            var isEditorHovered = ImNodes.isEditorHovered();

            ImNodes.miniMap(0.2f, ImNodesMiniMapLocation.BottomRight);
            ImNodes.endNodeEditor();

            if (ImNodes.isLinkCreated(linkA, linkB)) {
                var source = graph.findByOutput(linkA.get());
                var target = graph.findByInput(linkB.get());
                if (source != null && target != null && source.outputNodeId != target.nodeId) {
                    source.outputNodeId = target.nodeId;
                }
            }

            if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
                int hoveredNode = ImNodes.getHoveredNode();
                if (hoveredNode != -1) {
                    ImGui.openPopup("node_context");
                    ImGui.getStateStorage().setInt(ImGui.getID("delete_node_id"), hoveredNode);
                } else if (isEditorHovered) {
                    ImGui.openPopup("node_editor_context");
                }
            }

            if (ImGui.isPopupOpen("node_context")) {
                int targetNode = ImGui.getStateStorage().getInt(ImGui.getID("delete_node_id"));
                if (ImGui.beginPopup("node_context")) {
                    if (ImGui.button("Delete " + graph.nodes.get(targetNode).getName())) {
                        graph.nodes.remove(targetNode);
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endPopup();
                }
            }

            if (ImGui.beginPopup("node_editor_context")) {
                if (ImGui.button("Create New Node")) {
                    var node = graph.createGraphNode();
                    ImNodes.setNodeScreenSpacePos(node.nodeId, ImGui.getMousePosX(), ImGui.getMousePosY());
                    ImGui.closeCurrentPopup();
                }
                ImGui.endPopup();
            }
        }
        ImGui.end();
    }
}
