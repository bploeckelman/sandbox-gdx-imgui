package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxRuntimeException;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImDrawFlags;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NodeRenderer {

    private static final String TAG = NodeRenderer.class.getSimpleName();

    private enum State {
        INVALID, BEGIN, HEADER, CONTENT, INPUTS, MIDDLE, OUTPUTS, BACKGROUNDS, END;

        private static final EnumMap<State, Set<State>> VALID_TRANSITIONS = new EnumMap<>(State.class);
        static {
            // define valid state transitions: from -> set(to, ...)
            VALID_TRANSITIONS.put(INVALID, Set.of(BEGIN));
            VALID_TRANSITIONS.put(BEGIN, Set.of(HEADER, CONTENT));
            VALID_TRANSITIONS.put(HEADER, Set.of(CONTENT));
            VALID_TRANSITIONS.put(CONTENT, Set.of(INPUTS));
            VALID_TRANSITIONS.put(INPUTS, Set.of(MIDDLE));
            VALID_TRANSITIONS.put(MIDDLE, Set.of(OUTPUTS));
            VALID_TRANSITIONS.put(OUTPUTS, Set.of(END));
            VALID_TRANSITIONS.put(END, Set.of(BACKGROUNDS));
            VALID_TRANSITIONS.put(BACKGROUNDS, Set.of(INVALID));
        }
    }

    private final BlueprintEditor editor;

    private record SectionBounds(ImVec2 min, ImVec2 max) {
        public SectionBounds() {
            this(new ImVec2(), new ImVec2());
        }
        public static SectionBounds fromItemRect() {
            return new SectionBounds(ImGui.getItemRectMin(), ImGui.getItemRectMax());
        }
    }

    // TODO(brian):
    //  - add a 'NodeRenderOptions' class to encapsulate rendering settings like background colors, textures, etc...
    //  - add an override for `begin(node)` -> `begin(node, options)`
    //  - setup a `static class Default` here to encapsulate default options
    //  - could also not keep the current node/pin context and pass them into the appropriate consumers instead?

    private State state;
    private Node currentNode;
    private Pin currentPin;
    private boolean hasHeader;
    private ImVec4 headerColor;
    private SectionBounds nodeBounds;
    private SectionBounds headerBounds;
    private SectionBounds contentBounds;
    private SectionBounds inputsBounds;
    private SectionBounds middleBounds;
    private SectionBounds outputsBounds;
    private TextureRegion headerTexture;

    public NodeRenderer(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;
        this.state = State.INVALID;
        this.currentNode = null;
        this.currentPin = null;
        this.hasHeader = false;
        this.headerColor = new ImVec4(1, 1, 1, 1);
        this.headerTexture = null;
    }

    public void begin(Node node) {
        if (node == null) {
            Gdx.app.log(TAG, "NodeBuilder.begin: invalid node");
            return;
        }
        transitionTo(State.BEGIN);

        currentNode = node;
        hasHeader = false;
        headerBounds = new SectionBounds();

        NodeEditor.beginNode(currentNode.pointerId);
        ImGui.pushID(currentNode.pointerId);
        ImGui.beginGroup();
    }

    public void end() {
        transitionTo(State.END);

        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endNode();

        // update bounds for the 'node' section
        nodeBounds = SectionBounds.fromItemRect();

        // render custom node backgrounds
        // call order matters here, must be after `NodeEditor.endNode()` so the node is fully specified
        // otherwise `NodeEditor.getNodeBackgroundDrawList()` fails an imgui assertion
        transitionTo(State.BACKGROUNDS);
        var headerBgColor = ImColor.rgba("#23531cff");
        var separatorColor = ImColor.rgb("#8ea687");
        var contentBgColor = ImColor.rgba("#282c27ff");
        renderNodeBackgrounds(headerBgColor, separatorColor, contentBgColor);

        // clear the current node context
        currentNode = null;

        transitionTo(State.INVALID);
    }

    public void header(Runnable headerContentRenderer) {
        transitionTo(State.HEADER);

        hasHeader = true;
        ImGui.beginGroup();
        {
            headerContentRenderer.run();

            // add some vertical space
            ImGui.spacing();
        }
        ImGui.endGroup();
        headerBounds = SectionBounds.fromItemRect();
    }

    public void content() {
        transitionTo(State.CONTENT);

        // 3 columns/groups: inputs - middle - outputs
        ImGui.beginGroup();

        // start with some vertical space
        ImGui.spacing();
        ImGui.spacing();
    }

    public void endContent() {
        ImGui.endGroup();

        // update bounds for the 'content' section
        contentBounds = SectionBounds.fromItemRect();

        // TODO(brian): support optional footer?
    }

    public void inputPins(List<Pin> pins, Consumer<Pin> inputPinRenderer) {
        transitionTo(State.INPUTS);

        ImGui.beginGroup();
        {
            for (var pin : pins) {
                if (pin.io != Pin.IO.INPUT) {
                    Gdx.app.debug(TAG, STR."inputs(\{pin}): invalid, must be \{Pin.IO.INPUT} type, skipping...");
                    continue;
                }

                pin(pin);
                inputPinRenderer.accept(pin);
                endPin();
            }
        }
        ImGui.endGroup();

        // update bounds for the 'inputs' section
        inputsBounds = SectionBounds.fromItemRect();

        // everything in the 'content' section is in one row
        ImGui.sameLine();
    }

    public void middle(Runnable middleContentRenderer) {
        transitionTo(State.MIDDLE);

        ImGui.beginGroup();
        {
            middleContentRenderer.run();
        }
        ImGui.endGroup();

        // update bounds for the 'middle' section
        middleBounds = SectionBounds.fromItemRect();

        // everything in the 'content' section is in one row
        ImGui.sameLine();
    }

    public void outputPins(List<Pin> pins, Consumer<Pin> outputPinRenderer) {
        transitionTo(State.OUTPUTS);

        ImGui.beginGroup();
        {
            for (var pin : pins) {
                if (pin.io != Pin.IO.OUTPUT) {
                    Gdx.app.debug(TAG, STR."outputs(\{pin}): invalid, must be \{Pin.IO.OUTPUT} type, skipping...");
                    continue;
                }

                pin(pin);
                outputPinRenderer.accept(pin);
                endPin();
            }
        }
        ImGui.endGroup();

        // update bounds for the 'outputs' section
        outputsBounds = SectionBounds.fromItemRect();

        // done with the 'content' section, no need for `sameLine()`
    }

    private void transitionTo(State nextState) {
        var canTransition = State.VALID_TRANSITIONS.get(state).contains(nextState);
        if (!canTransition) {
            throw new GdxRuntimeException(STR."Invalid state transition: \{state} -> \{nextState}");
        }
        state = nextState;
    }

    /**
     * Render customized backgrounds for different sections of the node.
     * NOTE: this can only be called after NodeBuilder.end() has been called,
     *  because `ImGui.getNodeBackgroundDrawList()` requires that the node is fully specified
     */
    private void renderNodeBackgrounds(int headerBgColor, int separatorColor, int contentBgColor) {
        var bgDrawList = NodeEditor.getNodeBackgroundDrawList(currentNode.pointerId);
        var nodeSizeX = NodeEditor.getNodeSizeX(currentNode.pointerId);
        var nodeStyle = NodeEditor.getStyle();
        var nodeRounding = nodeStyle.getNodeRounding();
        var nodePadding = nodeStyle.getNodePadding();

        // header background - with adjustments to fit the full node width
        // NOTE: min/max are top-left/bottom-right corners in screen space
        float headerMinX = headerBounds.min.x - nodePadding.x;
        float headerMaxX = headerMinX + nodeSizeX;
        float headerMinY = headerBounds.min.y - nodePadding.y;
        float headerMaxY = headerMinY + (headerBounds.max.y - headerBounds.min.y + nodePadding.y);

        bgDrawList.addRectFilled(
            headerMinX, headerMinY, headerMaxX, headerMaxY,
            headerBgColor, nodeRounding, ImDrawFlags.RoundCornersTop);

        // content background
        bgDrawList.addRectFilled(
            contentBounds.min, contentBounds.max,
            contentBgColor, nodeRounding, ImDrawFlags.RoundCornersBottom);

        // header/content separator - draw after backgrounds so it overlaps
        bgDrawList.addLine(
            headerMinX, headerMaxY, headerMaxX, headerMaxY,
            separatorColor, 1);
    }


    private void pin(Pin pin) {
        if (currentNode == null) {
            Gdx.app.log(TAG, STR."pin(\{pin})[internal]: invalid node");
        }
        if (pin == null) {
            Gdx.app.log(TAG, "pin(null): invalid pin, must not be null");
            return;
        }

        currentPin = pin;

        NodeEditor.beginPin(currentPin.pointerId, currentPin.io.pinKind());
        ImGui.pushID(currentPin.pointerId);
        ImGui.beginGroup();
    }

    private void endPin() {
        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endPin();

        currentPin = null;
    }
}
