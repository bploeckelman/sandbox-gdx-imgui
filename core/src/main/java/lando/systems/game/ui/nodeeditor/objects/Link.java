package lando.systems.game.ui.nodeeditor.objects;

import static lando.systems.game.ui.nodeeditor.objects.Link.Default.*;

public class Link extends NodeEditorObject {

    static class Default {
        static final String PREFIX = "Link_";
    }

    public final Pin source;
    public final Pin target;

    public Link(Pin sourcePin, Pin targetPin) {
        super(nextLinkId());
        this.source = sourcePin;
        this.target = targetPin;
    }

    public String name() {
        return STR."\{PREFIX}\{id}";
    }

    @Override
    public String toString() {
        return STR."\{name()}[\{source}->\{target}]";
    }

    public String toLabel() {
        return STR."\{name()}##\{pointerId}";
    }
}
