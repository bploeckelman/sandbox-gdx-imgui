package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.Gdx;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import lando.systems.game.ui.Graph;
import lando.systems.game.ui.ImGuiCore;
import lando.systems.game.ui.NodeCanvas;
import lando.systems.game.ui.nodeeditor.objects.Link;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.NodeEditorObject;
import lando.systems.game.ui.nodeeditor.panels.EditorPane;
import lando.systems.game.ui.nodeeditor.panels.InfoPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class BlueprintEditor extends NodeCanvas {

    private static final String TAG = BlueprintEditor.class.getSimpleName();
    private static final String SETTINGS_FILE = "node-editor.json";

    public static final String URL = "https://github.com/thedmd/imgui-node-editor/tree/master/examples";
    public static final String REPO = "thedmd/imgui-node-editor";

    private final float touchTime = 1f;
    private final Map<Long, Float> nodeTouchTime ;
    private final Map<Long, NodeEditorObject> objectByPointerId;

    public final List<Node> nodes;
    public final List<Link> links;
    public final Graph graph;
    public final ImBoolean showOrdinals;

    public NodeEditorContext context;
    public InfoPane infoPane;
    public EditorPane editorPane;
    public long[] selectedNodes;
    public long[] selectedLinks;
    public int numSelectedNodes;
    public int numSelectedLinks;

    public BlueprintEditor(ImGuiCore imgui) {
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
        // NOTE: order matters here, editor context has to be
        //  created and set current before instantiating panes
        var config = new NodeEditorConfig();
        config.setSettingsFile(SETTINGS_FILE);
        context = NodeEditor.createEditor(config);
        NodeEditor.setCurrentEditor(context);

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
        updateSelections();
    }

    @Override
    public void render() {
        NodeEditor.setCurrentEditor(context);
        int mainWindowFlags = ImGuiWindowFlags.NoBringToFrontOnFocus
            | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowTitleAlign, new ImVec2(0.5f, 0.5f));
        ImGui.pushFont(imgui.getFont("Play-Regular.ttf"));
        ImGui.begin("Blueprint Node Editor", mainWindowFlags);
        {
            var cursorScreenPos = ImGui.getCursorScreenPos();
            var availableSize = ImGui.getContentRegionAvail();
            float infoWidth = (1 / 3f) * availableSize.x;
            float editorWidth = availableSize.x - infoWidth;
            float height = availableSize.y;

            ImGui.setNextWindowPos(cursorScreenPos.x, cursorScreenPos.y);
            ImGui.setNextWindowSize(infoWidth, height);
            infoPane.render();

            ImGui.sameLine();

            ImGui.setNextWindowPos(cursorScreenPos.x + infoWidth, cursorScreenPos.y);
            ImGui.setNextWindowSize(editorWidth, height);
            editorPane.render();
        }
        ImGui.end(); // "Blueprint Node Editor"
        ImGui.popFont();
        ImGui.popStyleVar(2);
    }

    // ------------------------------------------------------------------------
    // Utilities used by externally defined widgets
    // ------------------------------------------------------------------------

    public Optional<Node> findNode(long pointerId) {
        var object = objectByPointerId.get(pointerId);
        return Optional.ofNullable(object)
            .filter(Node.class::isInstance)
            .map(Node.class::cast);
    }

    public Optional<Link> findLink(long pointerId) {
        var object = objectByPointerId.get(pointerId);
        return Optional.ofNullable(object)
            .filter(Link.class::isInstance)
            .map(Link.class::cast);
    }

    public Stream<Node> getSelectedNodes() {
        return LongStream.of(selectedNodes)
            .mapToObj(objectByPointerId::get)
            .filter(Node.class::isInstance)
            .map(Node.class::cast);
    }

    public Stream<Link> getSelectedLinks() {
        return LongStream.of(selectedLinks)
            .mapToObj(objectByPointerId::get)
            .filter(Link.class::isInstance)
            .map(Link.class::cast);
    }

    public void zoomToContent() {
        NodeEditor.navigateToContent(1);
    }

    public void flow(Link link) {
        // TODO(brian):
    }

    public void updateSelections() {
        // selected objects are tracked together in native code,
        // so the selected id arrays are always the same length
        // and can be bigger than the actual counts for either type
        int totalCount = NodeEditor.getSelectedObjectCount();
        selectedNodes = new long[totalCount];
        selectedLinks = new long[totalCount];

        // populate the arrays with the selected object ids and get the counts by type
        numSelectedNodes = NodeEditor.getSelectedNodes(selectedNodes, totalCount);
        numSelectedLinks = NodeEditor.getSelectedLinks(selectedLinks, totalCount);

        // (optional) trim the arrays to the actual counts
        // this is not strictly necessary, but can be useful
        // so we don't need to remember to iterate using `numSelectedX` counts
        // instead of `selected[Nodes|Links].length` like one would expect
        if (numSelectedNodes < totalCount) {
            selectedNodes = Arrays.copyOf(selectedNodes, numSelectedNodes);
        }
        if (numSelectedLinks < totalCount) {
            selectedLinks = Arrays.copyOf(selectedLinks, numSelectedLinks);
        }
    }

    public boolean isSelected(Node node) {
        for (int i = 0 ; i < numSelectedNodes ; i++) {
            if (selectedNodes[i] == node.pointerId) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelected(Link link) {
        for (int i = 0 ; i < numSelectedLinks ; i++) {
            if (selectedLinks[i] == link.pointerId) {
                return true;
            }
        }
        return false;
    }

    public void select(Node node) {
        select(node, false);
    }

    public void select(Node node, boolean append) {
        NodeEditor.selectNode(node.pointerId, append);
    }

    public void deselect(Node node) {
        NodeEditor.deselectNode(node.pointerId);
    }

    public void navigateToSelection() {
        NodeEditor.navigateToSelection();
    }

    public void restoreNodeState(Node node) {
        NodeEditor.restoreNodeState(node.pointerId);
    }

    public boolean hasSelectionChanged() {
        return NodeEditor.hasSelectionChanged();
    }

    public void clearSelection() {
        NodeEditor.clearSelection();
    }

    // ------------------------------------------------------------------------
    // Node touch handling
    // ------------------------------------------------------------------------

    public void touchNode(Node node) {
        nodeTouchTime.put(node.pointerId, touchTime);
    }

    /**
     * Convert touch time for the specified node to a percent: 0..1
     */
    public float getTouchProgress(Node node) {
        float time = nodeTouchTime.getOrDefault(node.pointerId, 0f);
        if (time > 0f) {
            return (touchTime - time) / touchTime;
        }
        return time;
    }

    public void updateTouch(float dt) {
        // java's map interface doesn't allow direct modification
        // of values while iterating, so we'll do it this way...
        nodeTouchTime.replaceAll((id, time) -> (time > 0f) ? (time - dt) : time);
    }

    public void addNode(Node node) {
        var _existingNode = findNode(node.pointerId);
        if (_existingNode.isPresent()) {
            var existingNode = _existingNode.get();
            Gdx.app.log(TAG, STR."Failed to add, node already exists: \{existingNode}");
            return;
        }

        nodes.add(node);
        objectByPointerId.put(node.pointerId, node);
    }
}
