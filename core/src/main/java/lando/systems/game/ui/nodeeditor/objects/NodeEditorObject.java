package lando.systems.game.ui.nodeeditor.objects;

public abstract class NodeEditorObject {

    public final long pointerId;
    public final int id;

    public NodeEditorObject(int objectId) {
        this.pointerId = nextPointerId();
        this.id = objectId;
    }

    private static long POINTER_NEXT_ID = 1L;

    private static int NODE_NEXT_ID = 1;
    private static int PIN_NEXT_ID = 1;
    private static int LINK_NEXT_ID = 1;

    /**
     * @return a globally unique id for use with methods that take 'pointer id' args
     * that aren't actually pointers to native memory but rather just globally unique identifiers
     */
    public static long nextPointerId() {
        return POINTER_NEXT_ID++;
    }

    /**
     * @return an id, unique within the scope of nodes
     */
    public static int nextNodeId() {
        return NODE_NEXT_ID++;
    }

    /**
     * @return an id, unique within the scope of pins
     */
    public static int nextPinId() {
        return PIN_NEXT_ID++;
    }

    /**
     * @return an id, unique within the scope of links
     */
    public static int nextLinkId() {
        return LINK_NEXT_ID++;
    }
}
