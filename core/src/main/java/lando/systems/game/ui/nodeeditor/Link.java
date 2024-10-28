package lando.systems.game.ui.nodeeditor;

import static lando.systems.game.ui.nodeeditor.Link.Default.*;

public class Link extends NodeEditorObject {

    static class Default {
        static final String PREFIX = "Link#";
    }

    public final Pin source;
    public final Pin target;

    public Link(Pin sourcePin, Pin targetPin) {
        super(nextLinkId());
        this.source = sourcePin;
        this.target = targetPin;
    }

    @Override
    public String toString() {
        return STR."\{PREFIX}\{id}[\{source} -> \{target}]";
    }

    public String toLabel() {
        return STR."\{PREFIX}\{id}##\{pointerId}";
    }
}
