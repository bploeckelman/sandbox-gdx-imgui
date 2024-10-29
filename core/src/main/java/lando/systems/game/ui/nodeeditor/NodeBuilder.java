package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.imnodes.flag.ImNodesStyleVar;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.flag.ImDrawFlags;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;

public class NodeBuilder {

    private static final String TAG = NodeBuilder.class.getSimpleName();

    enum Stage { INVALID, BEGIN, HEADER, CONTENT, INPUT, MIDDLE, OUTPUT, END }

    private final BlueprintEditor editor;

    private Stage stage;
    private Node currentNode;
    private Pin currentPin;
    private boolean hasHeader;
    private ImVec4 headerColor;
    private ImVec2 nodeMin;
    private ImVec2 nodeMax;
    private ImVec2 headerMin;
    private ImVec2 headerMax;
    private ImVec2 contentMin;
    private ImVec2 contentMax;
    private TextureRegion headerTexture;

    public NodeBuilder(BlueprintEditor blueprintEditor) {
        this.editor = blueprintEditor;
        this.stage = Stage.INVALID;
        this.currentNode = null;
        this.currentPin = null;
        this.headerColor = new ImVec4(1, 1, 1, 1);
        this.hasHeader = false;
        this.nodeMin = new ImVec2();
        this.nodeMax = new ImVec2();
        this.headerMin = new ImVec2();
        this.headerMax = new ImVec2();
        this.contentMin = new ImVec2();
        this.contentMax = new ImVec2();
        this.headerTexture = null;
    }

    public Stage stage() {
        return stage;
    }

    public void begin(Node node) {
        hasHeader = false;
        headerMin.set(0, 0);
        headerMax.set(0, 0);

        currentNode = node;

        NodeEditor.pushStyleVar(ImNodesStyleVar.NodePadding, 8);
        NodeEditor.pushStyleVar(ImNodesStyleVar.NodeBorderThickness, 2);

        NodeEditor.beginNode(currentNode.pointerId);
        ImGui.pushID(currentNode.pointerId);

        setStage(Stage.BEGIN);
    }

    public void end() {
        setStage(Stage.END);

        NodeEditor.endNode();

        if (ImGui.isItemVisible()) {
            var drawList = NodeEditor.getNodeBackgroundDrawList(currentNode.pointerId);
            var halfBorderWidth = NodeEditor.getStyle().getNodeBorderWidth() * 0.5f;
            var alpha = ImGui.getStyle().getAlpha();

            var tempHeaderColor = ImColor.rgba(headerColor.x, headerColor.y, headerColor.z, alpha);
            if ((headerMax.x > headerMin.x) && (headerMax.y > headerMin.y) && headerTexture != null) {
                // TODO(brian): might either not need to worry about uvs or might need to use Texture instead
                var uv = new ImVec2(
                    (headerMax.x - headerMin.x) / (4 * headerTexture.getRegionWidth()),
                    (headerMax.y - headerMin.y) / (4 * headerTexture.getRegionHeight()));
                drawList.addImageRounded(
                    headerTexture.getTexture().getTextureObjectHandle(),
                    headerMin.x - (8 - halfBorderWidth),
                    headerMin.y - (4 - halfBorderWidth),
                    headerMax.x + (8 - halfBorderWidth),
                    headerMax.y,
                    0, 0, uv.x, uv.y, tempHeaderColor,
                    NodeEditor.getStyle().getNodeRounding(),
                    ImDrawFlags.RoundCornersTop);

                if (contentMin.y > headerMax.y) {
                    drawList.addLine(
                        headerMin.x - (8 - halfBorderWidth), headerMax.y - 0.5f,
                        headerMax.x + (8 - halfBorderWidth), headerMax.y - 0.5f,
                        ImColor.rgba(255, 255, 255, 96 / (3 * 255)), 1f);
                }
            }
        }

        ImGui.popID();
        NodeEditor.popStyleVar(2);

        currentNode = null;

        setStage(Stage.INVALID);
    }

    public void header(float r, float g, float b, float a) {
        headerColor.set(r, g, b, a);
        setStage(Stage.HEADER);
    }

    public void endHeader() {
        setStage(Stage.CONTENT);
    }

    public void input(Pin pin) {
        if (pin == null) {
            Gdx.app.log(TAG, "NodeBuilder.input: invalid pin");
            return;
        }

        if (stage == Stage.BEGIN) {
            setStage(Stage.CONTENT);
        }

        var applyPadding = (stage == Stage.INPUT);
        setStage(Stage.INPUT);

        if (applyPadding) {
            float availableWidth = ImGui.getContentRegionAvailX();
            ImGui.dummy(availableWidth * 0.05f, 0);
        }

        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        pin(pin);
    }

    public void endInput() {
        endPin();
    }

    public void middle() {
        if (stage == Stage.BEGIN) {
            setStage(Stage.CONTENT);
        }

        setStage(Stage.MIDDLE);
    }

    public void output(Pin pin) {
        if (pin == null) {
            Gdx.app.log(TAG, "NodeBuilder.output: invalid pin");
            return;
        }

        if (stage == Stage.BEGIN) {
            setStage(Stage.CONTENT);
        }

        var applyPadding = (stage == Stage.OUTPUT);
        setStage(Stage.OUTPUT);

        if (applyPadding) {
            // apply a 'spring' effect by adding a flexible space
            float availableWidth = ImGui.getContentRegionAvailX();
            ImGui.dummy(availableWidth * 0.05f, 0);
        }

        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
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
//        ImGui.beginGroup();
    }

    private void endPin() {
//        ImGui.endGroup();
        ImGui.popID();
        NodeEditor.endPin();

        currentPin = null;
    }

    private void setStage(Stage stage) {
        if (this.stage == stage) {
            return;
        }

        var prevStage = this.stage;
        this.stage = stage;

        switch (prevStage) {
            case BEGIN, CONTENT, END, INVALID: break;
            case HEADER: {
                headerMin.set(ImGui.getItemRectMin());
                headerMax.set(ImGui.getItemRectMax());

                ImGui.spacing();
            } break;
            case INPUT, OUTPUT: {
                NodeEditor.popStyleVar(2);
                ImGui.spacing();
            } break;
            case MIDDLE: {
                ImGui.spacing();
            } break;
        }

        switch (stage) {
            case BEGIN, INVALID: break;
            case HEADER: {
                if (hasHeader) {
                    ImGui.spacing();
                }
                hasHeader = true;
            } break;
            case CONTENT, MIDDLE: {
                ImGui.dummy(300, 0);
                ImGui.spacing();
            } break;
            case INPUT, OUTPUT: {
                NodeEditor.pushStyleVar(ImNodesStyleVar.PinOffset, 4);
                NodeEditor.pushStyleVar(ImNodesStyleVar.PinHoverRadius, 8);

                if (!hasHeader) {
                    ImGui.sameLine(ImGui.getStyle().getItemSpacingX());
                }
            } break;
            case END: {
                if (prevStage == Stage.INPUT) {
                    ImGui.sameLine(ImGui.getStyle().getItemInnerSpacingX());
                }
                if (prevStage != Stage.BEGIN) {
                    ImGui.spacing();
                }

                contentMin.set(ImGui.getItemRectMin());
                contentMax.set(ImGui.getItemRectMax());

                ImGui.spacing();

                nodeMin.set(ImGui.getItemRectMin());
                nodeMax.set(ImGui.getItemRectMax());
            } break;
        }
    }
}
