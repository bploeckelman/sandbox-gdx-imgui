package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImDrawFlags;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;

import static lando.systems.game.ui.nodeeditor.objects.Node.Default.*;

public class Node<T> extends NodeEditorObject {

    private static final int headerBgColor = ImColor.rgba("#23531cff");
    private static final int separatorColor = ImColor.rgb("#8ea687");
    private static final int contentBgColor = ImColor.rgba("#282c27ff");

    static class Default {
        static final String NAME_PREFIX = "Node_";
        static final ImVec2 SIZE = new ImVec2(300, 200);
        static final int COLOR = ImColor.rgba("#ffffffff");
    }

    public record SectionBounds(ImVec2 min, ImVec2 max) {
        public SectionBounds() {
            this(new ImVec2(), new ImVec2());
        }
        public void set(ImVec2 min, ImVec2 max) {
            this.min.set(min);
            this.max.set(max);
        }
        public static SectionBounds fromItemRect() {
            return new SectionBounds(ImGui.getItemRectMin(), ImGui.getItemRectMax());
        }
    }

    public record Bounds(
        SectionBounds node,
        SectionBounds header,
        SectionBounds content,
        SectionBounds inputs,
        SectionBounds middle,
        SectionBounds outputs
    ) {
        public Bounds() {
            this(new SectionBounds(), new SectionBounds(), new SectionBounds(), new SectionBounds(), new SectionBounds(), new SectionBounds());
        }
    }

    public final List<Pin> inputs = new ArrayList<>();
    public final List<Pin> outputs = new ArrayList<>();
    public final Bounds bounds = new Bounds();

    public String name;
    public ImVec2 size = new ImVec2(SIZE);
    public int color = COLOR;

    private String state = "";
    private String savedState = "";

    public ImString text = new ImString(1024);
    public NodeData<T> data;

    public Node(Class<T> dataClass) {
        this(STR."\{NAME_PREFIX}", dataClass);
    }

    public Node(String name, Class<T> dataClass) {
        super(nextNodeId());
        this.data = new NodeData<>(this, dataClass);
        this.name = name;
        if (this.name.equals(NAME_PREFIX)) {
            this.name += id;
        }

        // TEMP: the reflection approach for NodeData might be more hassle than its worth,
        // for now just set some values for testing
        if (data.value instanceof ImString str) {
            str.set("This is a long multiline input field for testing purposes. It should wrap and scroll horizontally as needed.");
        } else if (data.value instanceof Text txt) {
            txt.set("Hello, nodes!");
        }
    }

    public void render() {
        NodeEditor.beginNode(pointerId);
        ImGui.pushID(pointerId);
        ImGui.beginGroup();

        // header -----------------------------------------
        ImGui.beginGroup();
        ImGui.text(toString());
        ImGui.spacing();
        ImGui.spacing();
        ImGui.endGroup();
        bounds.header.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        // content ----------------------------------------
        ImGui.beginGroup();
        {
            ImGui.spacing();
            ImGui.spacing();

            // inputs
            ImGui.beginGroup();
            inputs.forEach(Pin::render);
            ImGui.endGroup();
            bounds.inputs.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

            ImGui.sameLine();

            // middle
            ImGui.beginGroup();
            if (data != null) {
                data.render();
            }
            ImGui.endGroup();
            bounds.middle.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

            ImGui.sameLine();

            // outputs
            ImGui.beginGroup();
            outputs.forEach(Pin::render);
            ImGui.endGroup();
            bounds.outputs.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());
        }
        ImGui.endGroup();
        bounds.content.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());


        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endNode();
        bounds.node.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        renderBackgrounds();
    }

    public boolean hasState() {
        return (state != null && !state.isBlank());
    }

    public String getState() {
        return state;
    }

    public boolean hasSavedState() {
        return (savedState != null && !savedState.isBlank());
    }

    public String getSavedState() {
        return savedState;
    }

    public void updateSavedState() {
        savedState = state;
    }

    public void restoreSavedState() {
        state = savedState;
    }

    public void clearSavedState() {
        savedState = "";
    }

    @Override
    public String toString() {
        return name.startsWith(NAME_PREFIX) ? name
            : STR."\{NAME_PREFIX}\{id}['\{name}']";
    }

    /**
     * @return a string representation of this node for display in the node editor
     *         this is a special representation because the '##{pointerId}' suffix
     *         is used for things like 'ImGui.selectable(label, isSelected)'
     *         where the suffix is not actually drawn in the node
     *         but is required to disambiguate between nodes that might have the same name
     */
    public String toLabel() {
        return STR."\{name}##\{pointerId}";
    }

    /**
     * NOTE: only call after `NodeEditor.endNode()` because `NodeEditor.getNodeBackgroundDrawList()` requires that the node be fully specified
     * This is also fiddly, and needs more work
     */
    private void renderBackgrounds() {
        var draw = NodeEditor.getNodeBackgroundDrawList(pointerId);
        var style = NodeEditor.getStyle();
        var padding = style.getNodePadding();
        var rounding = style.getNodeRounding() - 1f;
        var borderWidth = style.getNodeBorderWidth();
        var borderWidthHover = style.getHoveredNodeBorderWidth();
        var borderWidthSelect = style.getSelectedNodeBorderWidth();
        var isHovered = NodeEditor.getHoveredNode() == pointerId;
        var isSelected = NodeEditor.isNodeSelected(pointerId);
        var border = borderWidth;

        // header background - with adjustments to fit the full node width
        // NOTE: min/max are top-left/bottom-right corners in screen space
        float headerMinX = bounds.node.min.x + border;
        float headerMaxX = bounds.node.max.x - border;
        float headerMinY = bounds.node.min.y + border;
        float headerMaxY = headerMinY + (bounds.header.max.y - bounds.header.min.y);

        draw.addRectFilled(
            headerMinX, headerMinY, headerMaxX, headerMaxY,
            headerBgColor, rounding, ImDrawFlags.RoundCornersTop);

        // content background
        draw.addRectFilled(
            bounds.node.min.x + border, headerMaxY,
            bounds.node.max.x - border,
            bounds.node.max.y - border,
            contentBgColor, rounding, ImDrawFlags.RoundCornersBottom);

        // header/content separator - draw after backgrounds so it overlaps
        // must be some sampling thing going on here that we have to adjust by 0.5f
        draw.addLine(
            headerMinX - 0.5f, headerMaxY,
            headerMaxX - 0.5f, headerMaxY,
            separatorColor, 1);
    }
}
