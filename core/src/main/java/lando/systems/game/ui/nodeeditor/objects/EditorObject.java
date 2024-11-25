package lando.systems.game.ui.nodeeditor.objects;

public abstract class EditorObject {

    public enum Type { NODE, PIN, LINK }

    private static int NEXT_NODE_ID = 1;
    private static int NEXT_PIN_ID = 1;
    private static int NEXT_LINK_ID = 1;
    private static long NEXT_GLOBAL_ID = 1L;

    public final Type objectType;
    public final int objectId;
    public final long globalId;

    public EditorObject(Type objectType) {
        this.objectType = objectType;
        this.objectId = switch (objectType) {
            case NODE -> NEXT_NODE_ID++;
            case PIN -> NEXT_PIN_ID++;
            case LINK -> NEXT_LINK_ID++;
        };
        this.globalId = NEXT_GLOBAL_ID++;
    }
}
