package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImDrawFlags;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;

public class NodeBuilder {

    private static final String TAG = NodeBuilder.class.getSimpleName();

    private final BlueprintEditor editor;

    private record SectionBounds(ImVec2 min, ImVec2 max) {
        public SectionBounds() {
            this(new ImVec2(), new ImVec2());
        }
        public static SectionBounds fromItemRect() {
            return new SectionBounds(ImGui.getItemRectMin(), ImGui.getItemRectMax());
        }
    }

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

    // TODO(brian): change the pattern for each section to have the caller provide a callback
    //  where the callback includes all widget render calls for whatever content goes in each section
    //  that would simplify the usage code, preventing the need to call begin/end for each section

    public NodeBuilder(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;
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

        currentNode = node;
        hasHeader = false;
        headerBounds = new SectionBounds();

        NodeEditor.beginNode(currentNode.pointerId);
        ImGui.pushID(currentNode.pointerId);
        ImGui.beginGroup();
    }

    public void end() {
        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endNode();

//        if (ImGui.isItemVisible()) {
//            var drawList = NodeEditor.getNodeBackgroundDrawList(currentNode.pointerId);
//            var halfBorderWidth = NodeEditor.getStyle().getNodeBorderWidth() * 0.5f;
//            var alpha = ImGui.getStyle().getAlpha();
//
//            var tempHeaderColor = ImColor.rgba(headerColor.x, headerColor.y, headerColor.z, alpha);
//            if ((headerMax.x > headerMin.x) && (headerMax.y > headerMin.y) && headerTexture != null) {
//                // TODO(brian): might either not need to worry about uvs or might need to use Texture instead
//                var uv = new ImVec2(
//                    (headerMax.x - headerMin.x) / (4 * headerTexture.getRegionWidth()),
//                    (headerMax.y - headerMin.y) / (4 * headerTexture.getRegionHeight()));
//                drawList.addImageRounded(
//                    headerTexture.getTexture().getTextureObjectHandle(),
//                    headerMin.x - (8 - halfBorderWidth),
//                    headerMin.y - (4 - halfBorderWidth),
//                    headerMax.x + (8 - halfBorderWidth),
//                    headerMax.y,
//                    0, 0, uv.x, uv.y, tempHeaderColor,
//                    NodeEditor.getStyle().getNodeRounding(),
//                    ImDrawFlags.RoundCornersTop);
//
//                if (contentMin.y > headerMax.y) {
//                    drawList.addLine(
//                        headerMin.x - (8 - halfBorderWidth), headerMax.y - 0.5f,
//                        headerMax.x + (8 - halfBorderWidth), headerMax.y - 0.5f,
//                        ImColor.rgba(255, 255, 255, 96 / (3 * 255)), 1f);
//                }
//            }
//        }

        nodeBounds = SectionBounds.fromItemRect();
        currentNode = null;
    }

    /**
     * Render customized backgrounds for different sections of the node.
     * NOTE: this can only be called after NodeBuilder.end() has been called,
     *  because `ImGui.getNodeBackgroundDrawList()` requires that the node is fully specified
     */
    public void renderNodeBackgrounds(long nodePointerId, int headerBgColor, int contentBgColor) {
        var bgDrawList = NodeEditor.getNodeBackgroundDrawList(nodePointerId);
        var nodeStyle = NodeEditor.getStyle();
        var nodeRounding = nodeStyle.getNodeRounding();
        var nodePadding = nodeStyle.getNodePadding();
        var nodeSizeX = NodeEditor.getNodeSizeX(nodePointerId);

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
            ImColor.rgb("#8ea687"), 1);

    }

    public void header(float r, float g, float b, float a) {
        hasHeader = true;
        headerColor.set(r, g, b, a);
        ImGui.beginGroup();
    }

    public void endHeader() {
        // add some vertical space
        ImGui.spacing();
        ImGui.endGroup();
        headerBounds = SectionBounds.fromItemRect();
    }

    public void content() {
        // 3 columns/groups: inputs - middle - outputs
        ImGui.beginGroup();

        // start with some vertical space
        ImGui.spacing();
        ImGui.spacing();
    }

    public void endContent() {
        ImGui.endGroup();
        contentBounds = SectionBounds.fromItemRect();
    }

    public void inputs() {
        ImGui.beginGroup();
    }

    public void endInputs() {
        ImGui.endGroup();
        inputsBounds = SectionBounds.fromItemRect();
        ImGui.sameLine();
    }

    public void middle() {
        ImGui.beginGroup();
    }

    public void endMiddle() {
        ImGui.endGroup();
        middleBounds = SectionBounds.fromItemRect();
        ImGui.sameLine();
    }

    public void outputs() {
        ImGui.beginGroup();
    }

    public void endOutputs() {
        ImGui.endGroup();
        outputsBounds = SectionBounds.fromItemRect();
    }

    public void input(Pin pin) {
        if (pin == null) {
            Gdx.app.log(TAG, "NodeBuilder.input: invalid pin");
            return;
        }

        pin(pin);
    }

    public void endInput() {
        endPin();
    }

    public void output(Pin pin) {
        if (pin == null) {
            Gdx.app.log(TAG, "NodeBuilder.output: invalid pin");
            return;
        }

        pin(pin);
    }

    public void endOutput() {
        endPin();
    }

    private void pin(Pin pin) {
        if (currentNode == null || currentPin != null) {
            Gdx.app.log(TAG, "NodeBuilder.pin: invalid state");
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
