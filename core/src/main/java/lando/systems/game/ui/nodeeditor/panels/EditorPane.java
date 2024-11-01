package lando.systems.game.ui.nodeeditor.panels;

import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImLong;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;
import lando.systems.game.ui.nodeeditor.NodeRenderer;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;

public class EditorPane {

    private final BlueprintEditor editor;
    private final NodeRenderer nodeRenderer;

    public EditorPane(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;
        this.nodeRenderer = new NodeRenderer(editor);
        init();
    }

    public void init() {
        for (int i = 0; i < 3; i++) {
            var node = new Node();
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
        int editorWindowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar;
        if (ImGui.begin("Editor", editorWindowFlags)) {
            NodeEditor.begin("Node Editor");
            {
                // render the nodes
                for (var node : editor.nodes) {
                    nodeRenderer.begin(node);
                    {
                        nodeRenderer.header(() -> ImGui.text(node.toString()));
                        nodeRenderer.content();
                        {
                            nodeRenderer.inputPins(node.inputs, (pin) -> {
                                ImGui.text(STR."\{FontAwesomeIcons.MapPin}\{pin.name} ");
                            });

                            nodeRenderer.middle(() -> {
                                ImGui.text("Node");
                                ImGui.text("Content");
                                ImGui.text("Middle");
                            });

                            nodeRenderer.outputPins(node.outputs, (pin) -> {
                                ImGui.text(STR."\{FontAwesomeIcons.MapPin}\{pin.name} ");
                            });
                        }
                        nodeRenderer.endContent();
                    }
                    nodeRenderer.end();
                }

                // render the links
                for (var link : editor.links) {
                    NodeEditor.link(link.pointerId, link.source.pointerId, link.target.pointerId);
                }

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
                NodeEditor.endCreate();

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
                NodeEditor.endDelete();

                // handle the context menu separate from rendering
                NodeEditor.suspend();
                {
                    var nodePointerId = new ImLong();
                    var pinPointerId = new ImLong();
                    var linkPointerId = new ImLong();

                    if (NodeEditor.showNodeContextMenu(nodePointerId)) {
                        ImGui.openPopup("node-context");
                    } else if (NodeEditor.showPinContextMenu(pinPointerId)) {
                        ImGui.openPopup("pin-context");
                    } else if (NodeEditor.showLinkContextMenu(linkPointerId)) {
                        ImGui.openPopup("link-context");
                    } else if (NodeEditor.showBackgroundContextMenu()) {
                        ImGui.openPopup("create-node");
                    }

                    ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f);
                    if (ImGui.beginPopup("node-context")) {
                        var node = editor.findNode(nodePointerId.get()).orElse(null);

                        ImGui.text("Node Context Menu");
                        ImGui.separator();
                        if (node != null) {
                            ImGui.text(STR."Node: \{node.name}");
                            ImGui.text(STR."Inputs: \{node.inputs.size()}");
                            ImGui.text(STR."Outputs: \{node.outputs.size()}");
                            ImGui.separator();
                            if (ImGui.menuItem("Delete")) {
                                editor.removeNode(node);
                            }
                        } else {
                            ImGui.text("Unknown node");
                        }
                        ImGui.endPopup();
                    } else if (ImGui.beginPopup("pin-context")) {
                        var pin = editor.findPin(pinPointerId.get()).orElse(null);

                        ImGui.text("Pin Context Menu");
                        ImGui.separator();
                        if (pin != null) {
                            ImGui.text(STR."Pin: \{pin.name}");
                            ImGui.text(STR."Node: \{pin.node.name}");
                            ImGui.separator();
                            if (ImGui.menuItem("Delete")) {
                                editor.removePin(pin);
                            }
                        } else {
                            ImGui.text("Unknown pin");
                        }
                        ImGui.endPopup();
                    } else if (ImGui.beginPopup("link-context")) {
                        var link = editor.findLink(linkPointerId.get()).orElse(null);

                        ImGui.text("Link Context Menu");
                        ImGui.separator();
                        if (link != null) {
                            ImGui.text(STR."Link: \{link.pointerId}");
                            ImGui.text(STR."Source: \{link.source.pointerId}");
                            ImGui.text(STR."Target: \{link.target.pointerId}");
                            ImGui.separator();
                            if (ImGui.menuItem("Delete")) {
                                editor.removeLink(link);
                            }
                        } else {
                            ImGui.text("Unknown link");
                        }
                        ImGui.endPopup();
                    } else if (ImGui.beginPopup("create-node")) {
                        if (ImGui.selectable("Create Node")) {
                            // TODO(brian): flesh out the node creation menu substantially,
                            //  different types of nodes, different numbers of pins, etc...
                            var node = new Node();
                            editor.addNode(node);
                        }
                        ImGui.endPopup();
                    }
                    ImGui.popStyleVar();
                }
                NodeEditor.resume();
            }
            NodeEditor.end();
        }
        ImGui.end(); // "Blueprint Canvas"
    }
}
