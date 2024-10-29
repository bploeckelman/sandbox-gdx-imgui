package lando.systems.game.ui.nodeeditor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.extension.imnodes.flag.ImNodesStyleVar;
import imgui.extension.nodeditor.NodeEditor;
import imgui.flag.ImDrawFlags;
import lando.systems.game.ui.nodeeditor.objects.Node;

public class NodeBuilder {

    enum Stage { INVALID, BEGIN, HEADER, CONTENT, INPUT, MIDDLE, OUTPUT, END }

    private final BlueprintEditor editor;

    private Stage stage;
    private Node currentNode;
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

    public void begin() {
        hasHeader = false;
        headerMin.set(0, 0);
        headerMax.set(0, 0);

        currentNode = new Node();
        NodeEditor.pushStyleVar(ImNodesStyleVar.NodePadding, new ImVec4(8, 4, 8, 8));
        NodeEditor.beginNode(currentNode.pointerId);
        ImGui.pushID(currentNode.pointerId);

        stage = Stage.BEGIN;
    }

    public void end() {
        stage = Stage.END;

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
        NodeEditor.popStyleVar();

        editor.addNode(currentNode);
        currentNode = null;

        stage = Stage.INVALID;
    }

    public void header(float r, float g, float b, float a) {
        headerColor.set(r, g, b, a);
        stage = Stage.HEADER;
    }

    public void endHeader() {
        stage = Stage.CONTENT;
    }

    public void input() {
        // TODO(brian): ...
    }

    public void endInput() {
        // TODO(brian): ...
    }

    public void Middle() {
        // TODO(brian): ...
    }

    // TODO(brian): endMiddle()?

    public void output() {
        // TODO(brian): ...
    }

    public void endOutput() {
        // TODO(brian): ...
    }

    private void pin() {
        // TODO(brian): ...
    }

    private void endPin() {
        // TODO(brian): ...
    }

}
