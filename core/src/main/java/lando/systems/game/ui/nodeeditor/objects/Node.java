package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImColor;
import imgui.ImVec2;

import java.util.ArrayList;
import java.util.List;

import static lando.systems.game.ui.nodeeditor.objects.Node.Default.*;

public class Node extends NodeEditorObject {

    static class Default {
        static final String NAME_PREFIX = "Node#";
        static final ImVec2 SIZE = new ImVec2(300, 200);
        static final int COLOR = ImColor.rgba("#ffffffff");
    }

    public final List<Pin> inputs = new ArrayList<>();
    public final List<Pin> outputs = new ArrayList<>();

    public String name;
    public ImVec2 size = new ImVec2(SIZE);
    public int color = COLOR;

    private String state = "";
    private String savedState = "";

    public Node() {
        super(nextNodeId());
        this.name = STR."\{NAME_PREFIX}\{id}";
    }

    public Node(String name) {
        super(nextNodeId());
        this.name = name;
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
