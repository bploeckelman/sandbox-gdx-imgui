package lando.systems.game.ui.nodeeditor;

import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import lando.systems.game.ui.nodeeditor.objects.Pin;
import lando.systems.game.ui.nodeeditor.objects.PinType;

public class PinDesc {
    /**
     * {@link NodeEditorPinKind}
     */
    public final int kind;
    public final PinType type;

    public String label;

    /**
     * @param kind {@link NodeEditorPinKind}
     * @param type {@link PinType}
     */
    public PinDesc(int kind, PinType type) {
        this(kind, type, type.name());
    }

    /**
     * @param kind {@link NodeEditorPinKind}
     * @param type {@link PinType}
     * @param label the label of the pin
     */
    public PinDesc(int kind, PinType type, String label) {
        this.kind = kind;
        this.type = type;
        this.label = label;
    }
}
