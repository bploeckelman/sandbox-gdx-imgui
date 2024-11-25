package lando.systems.game.ui.nodeeditor.objects;

import java.util.HashMap;
import java.util.Map;

public class NodeProperties {
    public final Map<String, String> strings = new HashMap<>();
    public final Map<String, Integer> ints = new HashMap<>();
    public final Map<String, Float> floats = new HashMap<>();
    public final Map<String, Boolean> bools = new HashMap<>();

    public NodeProperties() {}

    public NodeProperties(NodeProperties src) {
        copyFrom(src);
    }

    public void copyFrom(NodeProperties src) {
        this.strings.clear();
        this.ints.clear();
        this.floats.clear();
        this.bools.clear();

        this.strings.putAll(src.strings);
        this.ints.putAll(src.ints);
        this.floats.putAll(src.floats);
        this.bools.putAll(src.bools);
    }
}
