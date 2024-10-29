package lando.systems.game.ui.nodeeditor.objects;

import imgui.ImColor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;

import static lando.systems.game.ui.nodeeditor.objects.Pin.Default.*;

public class Pin extends NodeEditorObject{

    static class Default {
        static final String NAME_PREFIX = "Pin#";
        static final int COLOR = ImColor.rgb("#cecece");
    }

    public enum Type { FLOW, BOOL, INT, FLOAT, STR, OBJ, FUNC }
    public enum IO { INPUT, OUTPUT;

        public int pinKind() {
            return switch (this) {
                case INPUT -> NodeEditorPinKind.Input;
                case OUTPUT -> NodeEditorPinKind.Output;
            };
        }
    }

    public final String name;
    public final Node node;
    public final IO io;

    public Type type;

    public int color = COLOR;

    public Pin(Node node, IO io) {
        this(node, Type.FLOW, io);
    }

    public Pin(String name, Node node, IO io) {
        this(name, node, Type.FLOW, io);
    }

    public Pin(Node node, Type type, IO io) {
        super(nextPinId());
        this.name = STR."\{NAME_PREFIX}\{id}";
        this.node = node;
        this.type = type;
        this.io = io;
        switch (io) {
            case INPUT -> node.inputs.add(this);
            case OUTPUT -> node.outputs.add(this);
        }
    }

    public Pin(String name, Node node, Type type, IO io) {
        super(nextPinId());
        this.name = name;
        this.node = node;
        this.type = type;
        this.io = io;
        switch (io) {
            case INPUT -> node.inputs.add(this);
            case OUTPUT -> node.outputs.add(this);
        }
    }

    public Link connectTo(Pin target) {
        return new Link(this, target);
    }

    @Override
    public String toString() {
        return name.startsWith(NAME_PREFIX) ? name
            : STR."\{NAME_PREFIX}\{id}['\{name}']";
    }

    public String toLabel() {
        return STR."\{name}##\{pointerId}";
    }
}
