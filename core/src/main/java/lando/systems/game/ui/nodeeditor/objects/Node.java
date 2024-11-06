package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;

import static lando.systems.game.ui.nodeeditor.objects.Node.Default.*;

public class Node<T> extends NodeEditorObject {

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
        SectionBounds outputs
    ) {
        public Bounds() {
            this(new SectionBounds(), new SectionBounds(), new SectionBounds(), new SectionBounds(), new SectionBounds());
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
    }

    public void render() {
        NodeEditor.beginNode(pointerId);
        ImGui.pushID(pointerId);
        ImGui.beginGroup();

        // header -----------------------------------------
        ImGui.beginGroup();
        ImGui.text(name);
        ImGui.separator();
        ImGui.endGroup();
        bounds.header.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        // inputs -----------------------------------------
        ImGui.beginGroup();
        inputs.forEach(Pin::render);
        ImGui.endGroup();
        bounds.inputs.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        // content ----------------------------------------
        ImGui.beginGroup();
        if (data != null) {
            data.render();
        }
        ImGui.endGroup();
        bounds.content.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        // outputs ----------------------------------------
        ImGui.beginGroup();
        outputs.forEach(Pin::render);
        ImGui.endGroup();
        bounds.outputs.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());

        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endNode();

        bounds.node.set(ImGui.getItemRectMin(), ImGui.getItemRectMax());
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
}
