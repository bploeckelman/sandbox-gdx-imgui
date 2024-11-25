package lando.systems.game.ui.nodeeditor;

import imgui.ImColor;
import lando.systems.game.ui.nodeeditor.objects.NodeProperties;

import java.util.ArrayList;
import java.util.List;

public class NodeDesc {

    public String type;
    public int color = ImColor.rgba(1f, 1f, 1f, 1f);
    public List<PinDesc> inputs = new ArrayList<>();
    public List<PinDesc> outputs = new ArrayList<>();
    public NodeProperties props = new NodeProperties();

}
