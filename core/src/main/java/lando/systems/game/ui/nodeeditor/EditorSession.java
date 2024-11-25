package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.Gdx;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import imgui.extension.nodeditor.NodeEditor;
import lando.systems.game.ui.nodeeditor.objects.EditorObject;
import lando.systems.game.ui.nodeeditor.objects.Link2;
import lando.systems.game.ui.nodeeditor.objects.Node2;
import lando.systems.game.ui.nodeeditor.objects.Pin;
import lando.systems.game.ui.nodeeditor.objects.Pin2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class EditorSession {

    private final Map<Long, EditorObject> objectByGlobalId;
    private final Map<Long, Float> nodeTouchTime;
    private final float touchTime = 1f;

    public final List<Node2> nodes;
    public final List<Link2> links;

    public long[] selectedNodes;
    public long[] selectedLinks;
    public int numSelectedNodes;
    public int numSelectedLinks;

    public EditorSession() {
        this.objectByGlobalId = new HashMap<>();
        this.nodeTouchTime = new TreeMap<>(LongComparators.NATURAL_COMPARATOR);
        this.nodes = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    // Editor object management -----------------------------------------------

    public void addNode(Node2 node) {
        var _existingNode = findNode(node.globalId);
        if (_existingNode.isPresent()) {
            var existingNode = _existingNode.get();
            Gdx.app.log(this.getClass().getSimpleName(), STR."Failed to add, node already exists: \{existingNode}");
            return;
        }

        nodes.add(node);
        objectByGlobalId.put(node.globalId, node);
    }

    public void addPin(Pin2 pin) {
        objectByGlobalId.put(pin.globalId, pin);
    }

    public void addLink(Link2 link) {
        links.add(link);
        objectByGlobalId.put(link.globalId, link);
    }

    public void removeNode(Node2 node) {
        nodes.remove(node);
        objectByGlobalId.remove(node.globalId);
    }

    public void removePin(Pin2 pin) {
        objectByGlobalId.remove(pin.globalId);
    }

    public void removeLink(Link2 link) {
        links.remove(link);
        objectByGlobalId.remove(link.globalId);
    }

    public boolean canConnect(Pin2 srcPin, Pin2 dstPin) {
        // TODO(brian): add constraints; single/multi connection, type matching, etc...
        return (srcPin.node.globalId != dstPin.node.globalId);
    }

    // Query methods ----------------------------------------------------------

    public Optional<Node2> findNode(long globalId) {
        var object = objectByGlobalId.get(globalId);
        return Optional.ofNullable(object)
            .filter(Node2.class::isInstance)
            .map(Node2.class::cast);
    }

    public Optional<Link2> findLink(long globalId) {
        var object = objectByGlobalId.get(globalId);
        return Optional.ofNullable(object)
            .filter(Link2.class::isInstance)
            .map(Link2.class::cast);
    }

    public Optional<Pin2> findPin(long globalId) {
        var object = objectByGlobalId.get(globalId);
        return Optional.ofNullable(object)
            .filter(Pin2.class::isInstance)
            .map(Pin2.class::cast);
    }

    // Selection methods ------------------------------------------------------

    public Stream<Node2> getSelectedNodes() {
        return LongStream.of(selectedNodes)
            .mapToObj(objectByGlobalId::get)
            .filter(Node2.class::isInstance)
            .map(Node2.class::cast);
    }

    public Stream<Link2> getSelectedLinks() {
        return LongStream.of(selectedLinks)
            .mapToObj(objectByGlobalId::get)
            .filter(Link2.class::isInstance)
            .map(Link2.class::cast);
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

    public boolean isSelected(Node2 node) {
        for (int i = 0 ; i < numSelectedNodes ; i++) {
            if (selectedNodes[i] == node.globalId) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelected(Link2 link) {
        for (int i = 0 ; i < numSelectedLinks ; i++) {
            if (selectedLinks[i] == link.globalId) {
                return true;
            }
        }
        return false;
    }

    public void select(Node2 node) {
        select(node, false);
    }

    public void select(Node2 node, boolean append) {
        NodeEditor.selectNode(node.globalId, append);
    }

    public void deselect(Node2 node) {
        NodeEditor.deselectNode(node.globalId);
    }

    public boolean hasSelectionChanged() {
        return NodeEditor.hasSelectionChanged();
    }

    public void clearSelection() {
        NodeEditor.clearSelection();
    }

    // Touch handling ---------------------------------------------------------

    public void touchNode(Node2 node) {
        nodeTouchTime.put(node.globalId, touchTime);
    }

    /**
     * Convert touch time for the specified node to a percent: 0..1
     */
    public float getTouchProgress(Node2 node) {
        float time = nodeTouchTime.getOrDefault(node.globalId, 0f);
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
}
