package lando.systems.game.ui.imnodes;

import imgui.ImColor;
import imgui.ImGui;
import imgui.extension.imnodes.ImNodes;
import imgui.extension.imnodes.ImNodesEditorContext;
import imgui.extension.imnodes.ImNodesStyle;
import imgui.extension.imnodes.flag.ImNodesCol;
import imgui.extension.imnodes.flag.ImNodesMiniMapLocation;
import imgui.extension.imnodes.flag.ImNodesPinShape;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImInt;
import lando.systems.game.Util;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.Graph;
import lando.systems.game.ui.ImGuiCore;
import lando.systems.game.ui.NodeCanvas;

public class CanvasImNodes extends NodeCanvas {

    private static final String TAG = CanvasImNodes.class.getSimpleName();
    private static final String PREF_EDITOR_STATE = "node-canvas-editor-state";
    private static final String URL = "https://github.com/Nelarius/imnodes/tree/master/example";
    private static final String REPO = "Nelarius/imnodes";

    private static class NodeStyle {
        static class Attrib {
            static final float paddingX = 20f;
            static final float paddingY = 10f;
            static final float borderThickness = 4f;
        }
        static class Color {
            static final int title = ImColor.rgb("#2e5266");
            static final int titleHovered = ImColor.rgb("#d3d0cb");
            static final int titleSelected = ImColor.rgb("#e2c044");
            static final int outline = ImColor.rgb("#7fa7aa");
        }
        static class Pin {
            static final float circleRadius = 6f;
            static final float quadSideLength = circleRadius * 2f;
        }
    }

    final Graph graph;
    final ImInt linkA;
    final ImInt linkB;

    ImNodesEditorContext context;
    ImNodesStyle style;
    String editorState;

    public CanvasImNodes(ImGuiCore imgui) {
        super(imgui);
        this.graph = new Graph();
        this.linkA = new ImInt();
        this.linkB = new ImInt();
        this.editorState = "";
    }

    @Override
    public void init() {
        ImNodes.createContext();
        context = ImNodes.editorContextCreate();

        ImNodes.styleColorsDark();
        style = ImNodes.getStyle();
        style.setNodePadding(NodeStyle.Attrib.paddingX, NodeStyle.Attrib.paddingY);
        style.setNodeBorderThickness(NodeStyle.Attrib.borderThickness);
        style.setPinCircleRadius(NodeStyle.Pin.circleRadius);
        style.setPinQuadSideLength(NodeStyle.Pin.quadSideLength);
        // TODO(brian): not sure how to set these up, need to review library code
//        style.setColors(ImNodesCol.TitleBar, NodeStyle.Color.title);
//        style.setColors(ImNodesCol.TitleBarHovered, NodeStyle.Color.titleHovered);
//        style.setColors(ImNodesCol.TitleBarSelected, NodeStyle.Color.titleSelected);
    }

    @Override
    public void dispose() {
        ImNodes.editorContextFree(context);
        ImNodes.destroyContext();
    }

    @Override
    public void render() {
        ImGui.pushFont(imgui.getFont("Play-Regular.ttf"));

        if (ImGui.begin(STR."[\{REPO}]")) {
            ImGui.alignTextToFramePadding();
            if (ImGui.button(STR."\{FontAwesomeIcons.Save}Save ")) {
                // TODO(brian): show a toast or popup modal or something to indicate it was saved
                save();
            }
            ImGui.sameLine();
            if (ImGui.button(STR."\{FontAwesomeIcons.FolderOpen}Load ")) {
                load();
            }
            ImGui.sameLine();
            if (ImGui.button(STR."\{FontAwesomeIcons.CodeBranch}\{REPO}/examples ")) {
                Util.openUrl(URL);
            }

            ImNodes.editorContextSet(context);
            ImNodes.beginNodeEditor();

            ImNodes.pushColorStyle(ImNodesCol.TitleBar, NodeStyle.Color.title);
            ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, NodeStyle.Color.titleHovered);
            ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, NodeStyle.Color.titleSelected);
            ImNodes.pushColorStyle(ImNodesCol.NodeOutline, NodeStyle.Color.outline);
            for (var node : graph.nodes.values()) {
                ImNodes.beginNode(node.nodeId);
                {
                    ImNodes.beginNodeTitleBar();
                    ImGui.text(node.getName());
                    ImNodes.endNodeTitleBar();

                    var hasIncoming = graph.nodes.values().stream().anyMatch(n -> n.outputNodeId == node.nodeId);
                    var inColor = hasIncoming ? ImColor.rgb("#ccffcc") : ImColor.rgb("#66ff66");
                    var inShape = hasIncoming ? ImNodesPinShape.CircleFilled : ImNodesPinShape.QuadFilled;
                    ImNodes.pushColorStyle(ImNodesCol.Pin, inColor);
                    ImNodes.beginInputAttribute(node.getInputPinId(), inShape);
                    ImGui.text("In");
                    ImNodes.endInputAttribute();
                    ImNodes.popColorStyle();

                    ImGui.sameLine();

                    var hasOutgoing = (node.outputNodeId != -1);
                    var outColor = hasOutgoing ? ImColor.rgb("#ccccff") : ImColor.rgb("#6666ff");
                    var outShape = hasOutgoing ? ImNodesPinShape.CircleFilled : ImNodesPinShape.QuadFilled;
                    ImNodes.pushColorStyle(ImNodesCol.Pin, outColor);
                    ImNodes.beginOutputAttribute(node.getOutputPinId(), outShape);
                    ImGui.text("Out");
                    ImNodes.endOutputAttribute();
                    ImNodes.popColorStyle();
                }
                ImNodes.endNode();
            }
            ImNodes.popColorStyle();
            ImNodes.popColorStyle();
            ImNodes.popColorStyle();
            ImNodes.popColorStyle();

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
                    if (ImGui.button(STR."Delete \{graph.nodes.get(targetNode).getName()}")) {
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

        ImGui.popFont();
    }

    // TODO(brian): load and save are broken currently, I think it has to do with 'dataLength'
    //  tried both string length (num characters) and byte length (num bytes)
    //  crashes in both cases so I'm clearly missing something
//    @Override
//    public void load() {
//        if (false) {
//            editorState = Util.getPref(PREF_EDITOR_STATE, String.class);
//            ImNodes.loadEditorStateFromIniString(context, editorState, editorState.getBytes().length);
//        }
//    }
//
//    @Override
//    public void save() {
//        if (false) {
//            editorState = ImNodes.saveEditorStateToIniString(context);
//            Util.putPref(PREF_EDITOR_STATE, editorState);
//        }
//    }
}
