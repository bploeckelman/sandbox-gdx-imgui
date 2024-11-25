package lando.systems.game.ui.nodeeditor.panels;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.extension.nodeditor.flag.NodeEditorStyleColor;
import imgui.extension.nodeditor.flag.NodeEditorStyleVar;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImLong;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;
import lando.systems.game.ui.nodeeditor.NodeDesc;
import lando.systems.game.ui.nodeeditor.NodeFactory;
import lando.systems.game.ui.nodeeditor.objects.Link2;
import lando.systems.game.ui.nodeeditor.objects.Node2;
import lando.systems.game.ui.nodeeditor.objects.Pin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorPane {

    private final BlueprintEditor editor;

    // right-click popup context menu constants and state
    private static class ContextMenu {
        ImLong nodeGlobalId = new ImLong();
        ImLong pinGlobalId = new ImLong();
        ImLong linkGlobalId = new ImLong();
        Pin newNodeLinkPin;
        Pin newLinkPin;

        private static class PopupIds {
            static final String NODE_NEW = "Create New Node";
            static final String NODE = "Node Context Menu";
            static final String PIN = "Pin Context Menu";
            static final String LINK = "Link Context Menu";
        }
    }

    private final ContextMenu contextMenu = new ContextMenu();
    private final Map<String, NodeDesc> nodeRegistry = new HashMap<>();

    public EditorPane(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;

        List.of(
              NodeFactory.printText()
//            , NodeFactory.printChoice()
//            , ...
        ).forEach(nodeDesc -> {
            if (nodeRegistry.containsKey(nodeDesc.type)) {
                throw new IllegalStateException(STR."Duplicate node type: \{nodeDesc.type}");
            }

            nodeRegistry.put(nodeDesc.type, nodeDesc);
        });
    }

    public void render() {
        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar;
        if (ImGui.begin("Editor", flags)) {
            NodeEditor.begin("Node Editor");
            setStyle();

            editor.session.nodes.forEach(Node2::render);
            editor.session.links.forEach(Link2::render);

            handleLinkCreation();
            handleDeletions();

            handleRightClickContextMenus();

            unsetStyle();
            NodeEditor.end();
        }
        ImGui.end(); // "Blueprint Canvas"
    }

    private void setStyle() {
        NodeEditor.pushStyleColor(NodeEditorStyleColor.NodeBg,        new ImVec4(1f, 1f, 1f, 0.0f));
        NodeEditor.pushStyleColor(NodeEditorStyleColor.NodeBorder,    new ImVec4( 0.6f,  0.6f,  0.6f, 0.8f));
        NodeEditor.pushStyleColor(NodeEditorStyleColor.PinRect,       new ImVec4( 0.24f, 0.6f, 1f, 0.6f));
        NodeEditor.pushStyleColor(NodeEditorStyleColor.PinRectBorder, new ImVec4( 0.24f, 0.6f, 1f, 0.6f));

        NodeEditor.pushStyleVar(NodeEditorStyleVar.NodePadding,             new ImVec4(6, 6, 6, 6));
        NodeEditor.pushStyleVar(NodeEditorStyleVar.NodeRounding,            5f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.NodeBorderWidth,         1.5f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.HoveredNodeBorderWidth,  2.5f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.SelectedNodeBorderWidth, 3.5f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.PinBorderWidth,          1f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.PinRadius,               5f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.LinkStrength,            0f);
        NodeEditor.pushStyleVar(NodeEditorStyleVar.SourceDirection,         new ImVec2(0.0f, 1.0f));
        NodeEditor.pushStyleVar(NodeEditorStyleVar.TargetDirection,         new ImVec2(0.0f, -1.0f));
    }

    private void unsetStyle() {
        NodeEditor.popStyleVar(7);
        NodeEditor.popStyleColor(4);
    }

    private void handleLinkCreation() {
        // handle creating links between node pins
        // TODO(brian): needs to be fleshed out
        if (NodeEditor.beginCreate()) {
            var a = new ImLong();
            var b = new ImLong();
            if (NodeEditor.queryNewLink(a, b)) {
                // find the pin for each
                var srcPin = editor.session.findPin(a.get()).orElse(null);
                var dstPin = editor.session.findPin(b.get()).orElse(null);

                if (srcPin != null && dstPin != null && srcPin != dstPin) {
                    var valid = true;
                    if (srcPin.kind == NodeEditorPinKind.Input
                     && dstPin.kind == NodeEditorPinKind.Output) {
                        // already correct, src(in) -> dst(out)
                    } else if (srcPin.kind == NodeEditorPinKind.Output
                            && dstPin.kind == NodeEditorPinKind.Input) {
                        // reversed, src(out) <- dst(in); swap pins
                        var temp = srcPin;
                        srcPin = dstPin;
                        dstPin = temp;
                    } else {
                        valid = false;
                    }

                    if (valid && editor.session.canConnect(srcPin, dstPin)) {
                        NodeEditor.acceptNewItem();
                        var link = srcPin.connectTo(dstPin);
                        editor.session.addLink(link);
                    } else {
                        NodeEditor.rejectNewItem();
                    }
                }
            }
        }
        NodeEditor.endCreate(); // TODO: should this be inside the `if`?
    }

    private void handleDeletions() {
        // handle deleting nodes and links
        NodeEditor.beginDelete();
        {
            var globalId = new ImLong();
            while (NodeEditor.queryDeletedNode(globalId)) {
                if (NodeEditor.acceptDeletedItem()) {
                    editor.session
                        .findNode(globalId.get())
                        .ifPresent(editor.session::removeNode);
                }
            }
            while (NodeEditor.queryDeletedLink(globalId)) {
                if (NodeEditor.acceptDeletedItem()) {
                    editor.session
                        .findLink(globalId.get())
                        .ifPresent(editor.session::removeLink);
                }
            }
        }
        // TODO: should this be inside the `if`?
        NodeEditor.endDelete();
    }

    private void handleRightClickContextMenus() {
        var openPopupPosition = ImGui.getMousePos();

        // NOTE: about reference frame switching:
        //  popup windows are not done in graph space, they're done in screen space.
        //  `suspend()` changes the positioning reference frame from "graph" to "screen"
        //  then all following calls are in screen space and `resume()` returns to reference frame
        NodeEditor.suspend();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f);

        // open the appropriate popup, if any, depending on the right-click context
        if (NodeEditor.showNodeContextMenu(contextMenu.nodeGlobalId)) {
            ImGui.openPopup(ContextMenu.PopupIds.NODE);
        } else if (NodeEditor.showPinContextMenu(contextMenu.pinGlobalId)) {
            ImGui.openPopup(ContextMenu.PopupIds.PIN);
        } else if (NodeEditor.showLinkContextMenu(contextMenu.linkGlobalId)) {
            ImGui.openPopup(ContextMenu.PopupIds.LINK);
        } else if (NodeEditor.showBackgroundContextMenu()) {
            ImGui.openPopup(ContextMenu.PopupIds.NODE_NEW);
            contextMenu.newNodeLinkPin = null;
        }

        // node context popup -------------------------------------------------
        if (ImGui.beginPopup(ContextMenu.PopupIds.NODE)) {
            ImGui.text("Node Context Menu");
            ImGui.separator();

            var nodeGlobalId = contextMenu.nodeGlobalId.get();
            editor.session.findNode(nodeGlobalId)
                .ifPresentOrElse(node -> {
                    ImGui.text(STR."ID: \{node.globalId}");
                    ImGui.text(STR."Node: \{node.label}");
                    ImGui.text(STR."Inputs: \{node.inputs.size()}");
                    ImGui.text(STR."Outputs: \{node.outputs.size()}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.session.removeNode(node);
                    }
                }, () -> ImGui.text(STR."Unknown node: \{nodeGlobalId}"));

            ImGui.endPopup();
        }

        // pin context popup --------------------------------------------------
        if (ImGui.beginPopup(ContextMenu.PopupIds.PIN)) {
            ImGui.text("Pin Context Menu");
            ImGui.separator();

            var pinGlobalId = contextMenu.pinGlobalId.get();
            editor.session.findPin(pinGlobalId)
                .ifPresentOrElse(pin -> {
                    ImGui.text(STR."ID: \{pin.globalId}");
                    ImGui.text(STR."Pin: \{pin.label}");
                    ImGui.text(STR."Node: \{pin.node.label}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.session.removePin(pin);
                    }
                }, () -> ImGui.text(STR."Unknown pin: \{pinGlobalId}"));

            ImGui.endPopup();
        }

        // link context popup -------------------------------------------------
        if (ImGui.beginPopup(ContextMenu.PopupIds.LINK)) {
            ImGui.text("Link Context Menu");
            ImGui.separator();

            var linkPointerId = contextMenu.linkGlobalId.get();
            editor.session.findLink(linkPointerId)
                .ifPresentOrElse(link -> {
                    ImGui.text(STR."ID: \{link.globalId}");
                    ImGui.text(STR."Source: \{link.src.label}");
                    ImGui.text(STR."Target: \{link.dst.label}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.session.removeLink(link);
                    }
                }, () -> ImGui.text(STR."Unknown link: \{linkPointerId}"));

            ImGui.endPopup();
        }

        // create new node popup ----------------------------------------------
        if (ImGui.beginPopup(ContextMenu.PopupIds.NODE_NEW)) {

            ImGui.text(ContextMenu.PopupIds.NODE_NEW);
            ImGui.separator();

            // spawn a node if user selects a type from the popup
            Node2 newNode = null;
            for (var desc : nodeRegistry.values()) {
                if (ImGui.menuItem(desc.type)) {
                    newNode = new Node2(desc);
                }
            }

            // if a new node was spawned, add it to the editor
            if (newNode != null) {
                editor.session.addNode(newNode);

                // position the new node near the right-click position
                NodeEditor.setNodePosition(newNode.globalId, openPopupPosition);

                // TODO: auto-connect a pin in the new node to a link that was dragged out
            }

            ImGui.endPopup();
        }

        ImGui.popStyleVar();
        NodeEditor.resume();
    }
}
