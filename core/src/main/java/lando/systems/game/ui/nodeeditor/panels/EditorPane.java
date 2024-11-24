package lando.systems.game.ui.nodeeditor.panels;

import com.badlogic.gdx.math.MathUtils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorStyleColor;
import imgui.extension.nodeditor.flag.NodeEditorStyleVar;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImLong;
import imgui.type.ImString;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;
import lando.systems.game.ui.nodeeditor.objects.Text;

public class EditorPane {

    private final BlueprintEditor editor;

    // right-click popup context menu constants and state
    private static class PopupIds {
        static final String NODE_NEW = "Create New Node";
        static final String NODE = "Node Context Menu";
        static final String PIN = "Pin Context Menu";
        static final String LINK = "Link Context Menu";
    }
    private static class ContextMenu {
        ImLong nodePointerId = new ImLong();
        ImLong pinPointerId = new ImLong();
        ImLong linkPointerId = new ImLong();
        Pin newNodeLinkPin;
        Pin newLinkPin;
    }
    private final ContextMenu contextMenu = new ContextMenu();

    public EditorPane(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;

        // initialize some test nodes
        for (int i = 0; i < 3; i++) {
            var clazz = MathUtils.randomBoolean() ? Text.class : ImString.class;
            var node = new Node<>(clazz);
            editor.addNode(node);

            for (int j = 0; j < 2; j++) {
                var iPin = new Pin(STR."\{i}-in-\{j}", node, Pin.IO.INPUT);
                var oPin = new Pin(STR."\{i}-out-\{j}", node, Pin.IO.OUTPUT);
                editor.addPin(iPin);
                editor.addPin(oPin);
            }
        }
    }

    public void render() {
        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar;
        if (ImGui.begin("Editor", flags)) {
            NodeEditor.begin("Node Editor");
            setStyle();

            for (var node : editor.nodes) {
                node.render();
            }

            for (var link : editor.links) {
                link.render();
            }

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
                var srcPin = editor.findPin(a.get()).orElse(null);
                var dstPin = editor.findPin(b.get()).orElse(null);

                if (srcPin != null && dstPin != null && srcPin != dstPin) {
                    var valid = true;
                    if (srcPin.io == Pin.IO.OUTPUT && dstPin.io == Pin.IO.INPUT) {
                        // already correct, src(out) -> dst(in)
                    } else if (srcPin.io == Pin.IO.INPUT && dstPin.io == Pin.IO.OUTPUT) {
                        // reversed, src(in) <- dst(out); swap pins
                        var temp = srcPin;
                        srcPin = dstPin;
                        dstPin = temp;
                    } else {
                        valid = false;
                    }

                    if (valid && editor.canConnect(srcPin, dstPin)) {
                        NodeEditor.acceptNewItem();
                        var link = srcPin.connectTo(dstPin);
                        editor.addLink(link);
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
            var pointerId = new ImLong();
            while (NodeEditor.queryDeletedNode(pointerId)) {
                if (NodeEditor.acceptDeletedItem()) {
                    editor.findNode(pointerId.get()).ifPresent(editor::removeNode);
                }
            }
            while (NodeEditor.queryDeletedLink(pointerId)) {
                if (NodeEditor.acceptDeletedItem()) {
                    editor.findLink(pointerId.get()).ifPresent(editor::removeLink);
                }
            }
        }
        // TODO: should this be inside the `if`?
        NodeEditor.endDelete();
    }

    private void handleRightClickContextMenus() {
        // NOTE: about reference frame switching:
        //  popup windows are not done in graph space, they're done in screen space.
        //  `suspend()` changes the positioning reference frame from "graph" to "screen"
        //  then all following calls are in screen space and `resume()` returns to reference frame
        NodeEditor.suspend();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f);

        var openPopupPosition = ImGui.getMousePos();

        // open the appropriate popup, if any, depending on the right-click context
        if (NodeEditor.showNodeContextMenu(contextMenu.nodePointerId)) {
            ImGui.openPopup(PopupIds.NODE);
        } else if (NodeEditor.showPinContextMenu(contextMenu.pinPointerId)) {
            ImGui.openPopup(PopupIds.PIN);
        } else if (NodeEditor.showLinkContextMenu(contextMenu.linkPointerId)) {
            ImGui.openPopup(PopupIds.LINK);
        } else if (NodeEditor.showBackgroundContextMenu()) {
            ImGui.openPopup(PopupIds.NODE_NEW);
            contextMenu.newNodeLinkPin = null;
        }

        // node context popup -------------------------------------------------
        if (ImGui.beginPopup(PopupIds.NODE)) {
            ImGui.text("Node Context Menu");
            ImGui.separator();

            var nodePointerId = contextMenu.nodePointerId.get();
            editor.findNode(nodePointerId)
                .ifPresentOrElse(node -> {
                    ImGui.text(STR."ID: \{node.pointerId}");
                    ImGui.text(STR."Node: \{node.name}");
                    ImGui.text(STR."Inputs: \{node.inputs.size()}");
                    ImGui.text(STR."Outputs: \{node.outputs.size()}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.removeNode(node);
                    }
                }, () -> ImGui.text(STR."Unknown node: \{nodePointerId}"));

            ImGui.endPopup();
        }

        // pin context popup --------------------------------------------------
        if (ImGui.beginPopup(PopupIds.PIN)) {
            ImGui.text("Pin Context Menu");
            ImGui.separator();

            var pinPointerId = contextMenu.pinPointerId.get();
            editor.findPin(pinPointerId)
                .ifPresentOrElse(pin -> {
                    ImGui.text(STR."ID: \{pin.pointerId}");
                    ImGui.text(STR."Pin: \{pin.name}");
                    ImGui.text(STR."Node: \{pin.node.name}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.removePin(pin);
                    }
                }, () -> ImGui.text(STR."Unknown pin: \{pinPointerId}"));

            ImGui.endPopup();
        }

        // link context popup -------------------------------------------------
        if (ImGui.beginPopup(PopupIds.LINK)) {
            ImGui.text("Link Context Menu");
            ImGui.separator();

            var linkPointerId = contextMenu.linkPointerId.get();
            editor.findLink(linkPointerId)
                .ifPresentOrElse(link -> {
                    ImGui.text(STR."ID: \{link.pointerId}");
                    ImGui.text(STR."Source: \{link.source.name}");
                    ImGui.text(STR."Target: \{link.target.name}");
                    ImGui.separator();

                    if (ImGui.menuItem("Delete")) {
                        editor.removeLink(link);
                    }
                }, () -> ImGui.text(STR."Unknown link: \{linkPointerId}"));

            ImGui.endPopup();
        }

        // create new node popup ----------------------------------------------
        if (ImGui.beginPopup(PopupIds.NODE_NEW)) {
            var newNodePosition = openPopupPosition;

            ImGui.text(PopupIds.NODE_NEW);
            ImGui.separator();

            // TODO(brian): enumerate types of nodes to create


            // TODO(brian): flesh out the node creation menu substantially,
            //  different types of nodes, different numbers of pins, etc...

            // TODO(brian): nodes should be created at the click position, not top-left

            if (ImGui.menuItem("Testing Node")) {
                var node = new Node<>(ImInt.class);
                editor.addNode(node);
            }
            if (ImGui.menuItem("Text Node")) {
                var node = new Node<>(ImString.class);
                editor.addNode(node);
            }

            ImGui.endPopup();
        }

        ImGui.popStyleVar();
        NodeEditor.resume();
    }
}
