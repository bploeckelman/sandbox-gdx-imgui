package lando.systems.game.ui.nodeeditor;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import lando.systems.game.ui.ImGuiCore;
import lando.systems.game.ui.NodeCanvas;
import lando.systems.game.ui.nodeeditor.objects.Link2;
import lando.systems.game.ui.nodeeditor.objects.Node2;
import lando.systems.game.ui.nodeeditor.panels.EditorPane;
import lando.systems.game.ui.nodeeditor.panels.InfoPane;

public class BlueprintEditor extends NodeCanvas {

    private static final String TAG = BlueprintEditor.class.getSimpleName();
    private static final String SETTINGS_FILE = "node-editor.json";

    public static final String URL = "https://github.com/thedmd/imgui-node-editor/tree/master/examples";
    public static final String REPO = "thedmd/imgui-node-editor";

    public final ImBoolean showOrdinals;
    public final ImBoolean showMetricsWindow;

    public InfoPane infoPane;
    public EditorPane editorPane;
    public EditorSession session;
    public NodeEditorContext context;

    public BlueprintEditor(ImGuiCore imgui) {
        super(imgui);
        this.showOrdinals = new ImBoolean();
        this.showMetricsWindow = new ImBoolean();
        this.session = new EditorSession();
    }

    @Override
    public void init() {
        // NOTE: order matters here, editor context has to be
        //  created and set current before instantiating panes
        var config = new NodeEditorConfig();
        config.setSettingsFile(SETTINGS_FILE);
        context = NodeEditor.createEditor(config);
        NodeEditor.setCurrentEditor(context);

        infoPane = new InfoPane(this);
        editorPane = new EditorPane(this);
    }

    @Override
    public void dispose() {
        NodeEditor.destroyEditor(context);
    }

    @Override
    public void update() {
        float dt = ImGui.getIO().getDeltaTime();
        session.updateTouch(dt);
        session.updateSelections();
        infoPane.update();
    }

    @Override
    public void render() {
        NodeEditor.setCurrentEditor(context);
        int mainWindowFlags = ImGuiWindowFlags.NoBringToFrontOnFocus
            | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowTitleAlign, new ImVec2(0.5f, 0.5f));
        ImGui.pushFont(imgui.getFont("Play-Regular.ttf"));
        ImGui.begin("Blueprint Node Editor", mainWindowFlags);
        {
            var cursorScreenPos = ImGui.getCursorScreenPos();
            var availableSize = ImGui.getContentRegionAvail();
            float infoWidth = (1 / 4f) * availableSize.x;
            float editorWidth = availableSize.x - infoWidth;
            float height = availableSize.y;

            ImGui.setNextWindowPos(cursorScreenPos.x, cursorScreenPos.y);
            ImGui.setNextWindowSize(infoWidth, height);
            infoPane.render();

            ImGui.sameLine();

            ImGui.setNextWindowPos(cursorScreenPos.x + infoWidth, cursorScreenPos.y);
            ImGui.setNextWindowSize(editorWidth, height);
            editorPane.render();
        }
        ImGui.end(); // "Blueprint Node Editor"
        ImGui.popFont();
        ImGui.popStyleVar(2);
    }

    public void zoomToContent() {
        NodeEditor.navigateToContent(1);
    }

    public void flow(Link2 link) {
        // TODO(brian):
    }

    public void navigateToSelection() {
        NodeEditor.navigateToSelection();
    }

    public void restoreNodeState(Node2 node) {
        NodeEditor.restoreNodeState(node.globalId);
    }
}
