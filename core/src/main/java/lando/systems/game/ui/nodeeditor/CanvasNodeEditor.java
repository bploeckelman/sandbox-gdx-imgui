package lando.systems.game.ui.nodeeditor;

import com.github.tommyettinger.ds.support.sort.LongComparators;
import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.type.ImBoolean;
import lando.systems.game.Util;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.Graph;
import lando.systems.game.ui.ImGuiCore;
import lando.systems.game.ui.NodeCanvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class CanvasNodeEditor extends NodeCanvas {

    private static final String TAG = CanvasNodeEditor.class.getSimpleName();
    private static final String URL = "https://github.com/thedmd/imgui-node-editor/tree/master/examples";
    private static final String REPO = "thedmd/imgui-node-editor";
    private static final String SETTINGS_FILE = "node-editor.json";

    private final Map<Long, Float> nodeTouchTime ;
    private final float touchTime = 1f;

    final Graph graph;

    final ImBoolean showOrdinals;

    final Map<Long, NodeEditorObject> objectByPointerId;
    final List<Node> nodes;
    final List<Link> links;

    NodeEditorContext context;
    InfoPane infoPane;
    EditorPane editorPane;
    long[] selectedNodes;
    long[] selectedLinks;
    int numSelectedNodes;
    int numSelectedLinks;

    public CanvasNodeEditor(ImGuiCore imgui) {
        super(imgui);
        this.nodeTouchTime = new TreeMap<>(LongComparators.NATURAL_COMPARATOR);
        this.showOrdinals = new ImBoolean();
        this.objectByPointerId = new HashMap<>();
        this.nodes = new ArrayList<>();
        this.links = new ArrayList<>();
        this.selectedNodes = new long[0];
        this.selectedLinks = new long[0];

        // TODO(brian): remove me in lieu of direct management of nodes/links
        this.graph = new Graph();
    }

    @Override
    public void init() {
        var config = new NodeEditorConfig();
        config.setSettingsFile(SETTINGS_FILE);
        context = NodeEditor.createEditor(config);

        infoPane = new InfoPane(this);
        editorPane = new EditorPane(this);
    }

    @Override
    public void dispose() {
        NodeEditor.destroyEditor(context);
    }

    @Override
    public void update() {
        float dt = ImGui.getIO().getDeltaTime();
        updateTouch(dt);
    }

    @Override
    public void render() {
        ImGui.pushFont(imgui.getFont("Play-Regular.ttf"));

        if (ImGui.begin(STR."[\{REPO}]")) {
            ImGui.alignTextToFramePadding();

            ImGui.text("FPS: %.1f".formatted(ImGui.getIO().getFramerate()));

            ImGui.beginChild("main-menu");
            {
                if (ImGui.button(STR."\{FontAwesomeIcons.Save}Save ")) {
                    // TODO(brian): show a toast or popup modal or something to indicate it was saved
                    save();
                }
                ImGui.sameLine();
                if (ImGui.button(STR."\{FontAwesomeIcons.FolderOpen}Load ")) {
                    load();
                }
                ImGui.sameLine();
                if (ImGui.button(STR."\{FontAwesomeIcons.SearchLocation}Zoom ")) {
                    zoomToContent();
                }
                ImGui.sameLine();
                if (ImGui.button(STR."\{FontAwesomeIcons.CodeBranch}\{REPO}/examples ")) {
                    Util.openUrl(URL);
                }
            }
            ImGui.endChild();

            // TODO(brian): port split pane widget and use for info and editor panes
            ImGui.beginChild("main-content");
            {
                float availableWidth = ImGui.getContentRegionAvailX();
                float infoWidth = (1 / 3f) * availableWidth;
                float editorWidth = availableWidth - infoWidth;

                infoPane.render(infoWidth);
                editorPane.render(editorWidth);
            }
            ImGui.endChild();
        }
        ImGui.end();

        ImGui.popFont();
    }

    // ------------------------------------------------------------------------
    // Utilities used by externally defined widgets
    // ------------------------------------------------------------------------

    Optional<Node> findNode(long pointerId) {
        var object = objectByPointerId.get(pointerId);
        return Optional.ofNullable(object)
            .filter(Node.class::isInstance)
            .map(Node.class::cast);
    }

    Optional<Link> findLink(long pointerId) {
        var object = objectByPointerId.get(pointerId);
        return Optional.ofNullable(object)
            .filter(Link.class::isInstance)
            .map(Link.class::cast);
    }

    Stream<Node> getSelectedNodes() {
        return LongStream.of(selectedNodes)
            .mapToObj(objectByPointerId::get)
            .filter(Node.class::isInstance)
            .map(Node.class::cast);
    }

    Stream<Link> getSelectedLinks() {
        return LongStream.of(selectedLinks)
            .mapToObj(objectByPointerId::get)
            .filter(Link.class::isInstance)
            .map(Link.class::cast);
    }

    void zoomToContent() {
        NodeEditor.navigateToContent(1);
    }

    void flow(Link link) {
        // TODO(brian):
    }

    void updateSelections() {
        int count = NodeEditor.getSelectedObjectCount();
        selectedNodes = new long[count];
        selectedLinks = new long[count];
        numSelectedNodes = NodeEditor.getSelectedNodes(selectedNodes, count);
        numSelectedLinks = NodeEditor.getSelectedLinks(selectedLinks, count);
    }

    boolean isSelected(Node node) {
        for (int i = 0 ; i < numSelectedNodes ; i++) {
            if (selectedNodes[i] == node.pointerId) {
                return true;
            }
        }
        return false;
    }

    boolean isSelected(Link link) {
        for (int i = 0 ; i < numSelectedLinks ; i++) {
            if (selectedLinks[i] == link.pointerId) {
                return true;
            }
        }
        return false;
    }

    void select(Node node) {
        select(node, false);
    }

    void select(Node node, boolean append) {
        NodeEditor.selectNode(node.pointerId, append);
    }

    void deselect(Node node) {
        NodeEditor.deselectNode(node.pointerId);
    }

    void navigateToSelection() {
        NodeEditor.navigateToSelection();
    }

    void restoreNodeState(Node node) {
        NodeEditor.restoreNodeState(node.pointerId);
    }

    boolean hasSelectionChanged() {
        return NodeEditor.hasSelectionChanged();
    }

    void clearSelection() {
        NodeEditor.clearSelection();
    }

    // ------------------------------------------------------------------------
    // Node touch handling
    // ------------------------------------------------------------------------

    void touchNode(Node node) {
        nodeTouchTime.put(node.pointerId, touchTime);
    }

    /**
     * Convert touch time for the specified node to a percent: 0..1
     */
    float getTouchProgress(Node node) {
        float time = nodeTouchTime.getOrDefault(node.pointerId, 0f);
        if (time > 0f) {
            return (touchTime - time) / touchTime;
        }
        return time;
    }

    void updateTouch(float dt) {
        // java's map interface doesn't allow direct modification
        // of values while iterating, so we'll do it this way...
        nodeTouchTime.replaceAll((id, time) -> (time > 0f) ? (time - dt) : time);
    }
}
