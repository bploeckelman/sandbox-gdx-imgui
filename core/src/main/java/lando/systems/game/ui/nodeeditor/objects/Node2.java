package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiInputTextCallbackData;
import imgui.ImVec2;
import imgui.callback.ImGuiInputTextCallback;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import lando.systems.game.ui.nodeeditor.NodeDesc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node2 extends EditorObject {

    public static class Bounds {
        public final ImVec2 min = new ImVec2();
        public final ImVec2 max = new ImVec2();

        public Bounds() {}
        public void set(ImVec2 min, ImVec2 max) {
            this.min.set(min);
            this.max.set(max);
        }
        public void setFromItemRect() {
            set(ImGui.getItemRectMin(), ImGui.getItemRectMax());
        }
    }
    public enum Section { NODE, HEADER, CONTENT, INPUTS, MIDDLE, OUTPUTS }

    public String label;
    public int color;

    public final NodeProperties props;
    public final List<Pin2> inputs;
    public final List<Pin2> outputs;
    public final Map<Section, Bounds> bounds;

    public Node2(NodeDesc desc) {
        super(EditorObject.Type.NODE);

        this.label = desc.type;
        this.color = desc.color;
        this.props = new NodeProperties(desc.props);
        this.inputs = new ArrayList<>();
        for (var pinDesc : desc.inputs) {
            inputs.add(new Pin2(this, pinDesc));
        }
        this.outputs = new ArrayList<>();
        for (var pinDesc : desc.outputs) {
            outputs.add(new Pin2(this, pinDesc));
        }
        this.bounds = new HashMap<>();
        for (var section : Section.values()) {
            bounds.put(section, new Bounds());
        }
    }

    public void render() {
        NodeEditor.beginNode(globalId);
        ImGui.pushID(globalId);
        ImGui.beginGroup();

        // header -----------------------------------------
        ImGui.beginGroup();
        ImGui.text(label);
        ImGui.spacing();
        ImGui.spacing();
        ImGui.endGroup();
        bounds.get(Section.HEADER).setFromItemRect();

        // content ----------------------------------------
        ImGui.beginGroup();
        {
            ImGui.spacing();

            // inputs
            ImGui.beginGroup();
            inputs.forEach(Pin2::render);
            ImGui.endGroup();
            bounds.get(Section.INPUTS).setFromItemRect();

            ImGui.sameLine();

            // middle
            ImGui.beginGroup();
            for (var prop : props.strings.entrySet()) {
                var key = prop.getKey();
                switch (key) {
                    case "Text" -> {
                        var inputKey = key.toLowerCase().replaceAll(" ", "_");
                        var inputLabel = STR."##prop-\{inputKey}-\{toLabel()}";
                        var inputString = new ImString(prop.getValue());
                        var inputCallback = new ImGuiInputTextCallback() {
                            @Override
                            public void accept(ImGuiInputTextCallbackData data) {
                                prop.setValue(data.getBuf());
                            }
                        };

                        var size = ImGui.calcTextSize(prop.getValue());
                        var margin = size.x * 0.1f;
                        ImGui.pushItemWidth(size.x + margin);
                        ImGui.inputText(inputLabel, inputString, ImGuiInputTextFlags.CallbackEdit, inputCallback);
                        ImGui.popItemWidth();
                    }
                    default -> {
                        ImGui.text(key);
                        ImGui.sameLine();
                        ImGui.text(prop.getValue());
                    }
                }
            }
            ImGui.endGroup();
            bounds.get(Section.MIDDLE).setFromItemRect();

            ImGui.sameLine();

            // outputs
            ImGui.beginGroup();
            outputs.forEach(Pin2::render);
            ImGui.endGroup();
            bounds.get(Section.OUTPUTS).setFromItemRect();
        }
        ImGui.endGroup();
        bounds.get(Section.CONTENT).setFromItemRect();


        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endNode();
        bounds.get(Section.NODE).setFromItemRect();

        renderBackgrounds();
    }

    private void renderBackgrounds() {
        var draw = NodeEditor.getNodeBackgroundDrawList(globalId);
        var style = NodeEditor.getStyle();
        var padding = style.getNodePadding();
        var rounding = style.getNodeRounding() - 1f;
        var borderWidth = style.getNodeBorderWidth();
        var borderWidthHover = style.getHoveredNodeBorderWidth();
        var borderWidthSelect = style.getSelectedNodeBorderWidth();
        var isHovered = NodeEditor.getHoveredNode() == globalId;
        var isSelected = NodeEditor.isNodeSelected(globalId);
        var border = borderWidth;

        // header background - with adjustments to fit the full node width
        // NOTE: min/max are top-left/bottom-right corners in screen space
        float headerMinX = bounds.get(Section.NODE).min.x + border;
        float headerMaxX = bounds.get(Section.NODE).max.x - border;
        float headerMinY = bounds.get(Section.NODE).min.y + border;
        float headerMaxY = headerMinY + (bounds.get(Section.HEADER).max.y - bounds.get(Section.HEADER).min.y);

        draw.addRectFilled(
            headerMinX, headerMinY, headerMaxX, headerMaxY,
            Color.headerBackground, rounding, ImDrawFlags.RoundCornersTop);

        // content background
        draw.addRectFilled(
            bounds.get(Section.NODE).min.x + border, headerMaxY,
            bounds.get(Section.NODE).max.x - border,
            bounds.get(Section.NODE).max.y - border,
            Color.contentBackground, rounding, ImDrawFlags.RoundCornersBottom);

        // header/content separator - draw after backgrounds so it overlaps
        // must be some sampling thing going on here that we have to adjust by 0.5f
        draw.addLine(
            headerMinX - 0.5f, headerMaxY,
            headerMaxX - 0.5f, headerMaxY,
            Color.separator, 1);
    }

    public String toLabel() {
        return STR."Node\{objectId}#\{globalId}";
    }

    @Override
    public String toString() {
        return STR."\{toLabel()}: '\{label}'";
    }

    private static class Color {
        private static final int headerBackground = ImColor.rgba("#23531cff");
        private static final int contentBackground = ImColor.rgba("#282c27ff");
        private static final int separator= ImColor.rgb("#8ea687");
    }
}

