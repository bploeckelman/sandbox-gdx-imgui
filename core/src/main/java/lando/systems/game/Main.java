package lando.systems.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisTable;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.shared.ImGuiPlatform;
import lando.systems.game.ui.ImGuiCore;
import lando.systems.game.ui.NodeCanvas;
import lando.systems.game.ui.imnodes.CanvasImNodes;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;
import space.earlygrey.shapedrawer.ShapeDrawer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private static final String TAG = Main.class.getSimpleName();

    public static Main game;

    public SpriteBatch batch;
    public ShapeDrawer shapes;
    public OrthographicCamera windowCamera;
    public InputMultiplexer inputMux;
    public Preferences preferences;

    Color backgroundColor;
    TextureAtlas atlas;
    Texture gdx;
    Texture pixel;
    TextureRegion pixelRegion;

    // NOTE: for convenience to group all these uiskin atlas textures
    public record RadioButtonTextures(
        TextureRegion normal,
        TextureRegion over,
        TextureRegion down,
        TextureRegion tick,
        TextureRegion tickDisabled,
        TextureRegion focusBorder,
        TextureRegion errorBorder
    ) {}
    public RadioButtonTextures radioBtnTextures;

    Skin skin;
    Stage stage;
    VisTable root;

    ImGuiCore imgui;
    CanvasImNodes canvasImNodes;
    BlueprintEditor canvasNodeEditor;
    Rectangle view = new Rectangle();

    NodeCanvas.Type nodeCanvasType = NodeCanvas.Type.NODE_EDITOR;
    NodeCanvas nodeCanvas;

    public Main(ImGuiPlatform imGuiPlatform) {
        Main.game = this;
        this.imgui = new ImGuiCore(imGuiPlatform);
        this.canvasImNodes = new CanvasImNodes(imgui);
        this.canvasNodeEditor = new BlueprintEditor(imgui);
    }

    @Override
    public void create() {
        Util.init();

        batch = new SpriteBatch();
        shapes = new ShapeDrawer(batch);
        inputMux = new InputMultiplexer();
        windowCamera = new OrthographicCamera();
        windowCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        windowCamera.update();

        loadAssets();
        loadScene2d();

        imgui.init();
        canvasImNodes.init();
        canvasNodeEditor.init();
        setActiveNodeCanvas(nodeCanvasType);
    }

    private void loadScene2d() {
        VisUI.load(skin);
        VisUI.setDefaultTitleAlign(Align.center);

        stage = new Stage();
        //stage.setDebugTableUnderMouse(Table.Debug.all);

        root = new VisTable(true);
        root.setFillParent(true);
        stage.addActor(root);

        setDefaults();
        populateRoot();

        inputMux.addProcessor(stage);
        Gdx.input.setInputProcessor(inputMux);
    }

    private void loadAssets() {
        backgroundColor = new Color(0.15f, 0.15f, 0.2f, 1f);
        gdx = new Texture("libgdx.png");

        // create a single pixel texture and associated region
        var pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        {
            pixmap.setColor(Color.WHITE);
            pixmap.drawPixel(0, 0);
            pixmap.drawPixel(1, 0);
            pixmap.drawPixel(0, 1);
            pixmap.drawPixel(1, 1);

            pixel = new Texture(pixmap);
            pixelRegion = new TextureRegion(pixel);
        }
        pixmap.dispose();
        shapes.setTextureRegion(pixelRegion);

        var skinFile = "ui/skin-talos/uiskin";
        atlas = new TextureAtlas(Gdx.files.internal(STR."\{skinFile}.atlas"));
        radioBtnTextures = new RadioButtonTextures(
            atlas.findRegion("vis-radio"),
            atlas.findRegion("vis-radio-over"),
            atlas.findRegion("vis-radio-down"),
            atlas.findRegion("vis-radio-tick"),
            atlas.findRegion("vis-radio-tick-disabled"),
            atlas.findRegion("border-circle"),
            atlas.findRegion("border-circle-error")
        );

        skin = new Skin(Gdx.files.internal(STR."\{skinFile}.json"), atlas);
        skin.addRegions(atlas);
    }

    public void setDefaults() {
        root.defaults().top().left();
    }

    public void populateRoot() {
        root.clearChildren();

//        menu = new MainMenu(stage, skin);
//        workspace = new VisTable(true);
//        tools = new Toolbar(stage, skin);
//
//        var toolsWidth = Value.percentWidth(1 / 4f, root);
//        var emptyWidth = Value.percentWidth(3 / 4f, root);
//        workspace.add(tools).width(toolsWidth).growY();
//        workspace.add().width(emptyWidth).grow();
//
//        root.add(menu).growX();
//        root.row();
//        root.add(workspace).grow();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        PopupMenu.removeEveryMenu(stage);

        stage.getViewport().update(width, height, true);

        var resizeEvent = new Event();
        for (var actor : stage.getActors()) {
            actor.fire(resizeEvent);
        }
    }

    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            setActiveNodeCanvas(
                nodeCanvasType == NodeCanvas.Type.NODE_EDITOR
                    ? NodeCanvas.Type.IM_NODES
                    : NodeCanvas.Type.NODE_EDITOR);
        }

        stage.act(dt);
        canvasNodeEditor.update();

        windowCamera.update();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        update(dt);

        ScreenUtils.clear(backgroundColor);

        float margin = 50;
        float x = windowCamera.viewportWidth - gdx.getWidth() - margin;

        batch.setProjectionMatrix(windowCamera.combined);
        batch.begin();
        batch.draw(gdx, x, margin);
        batch.end();

        stage.draw();

        imgui.startFrame();
        {
            // TODO(brian): most of the layout here can be replaced with docking
            var viewport = ImGui.getMainViewport();
            view.set(
                viewport.getPosX(), viewport.getPosY(),
                viewport.getSizeX(), viewport.getSizeY());

            float col1 = 0.6f;
            float col2 = 1 - col1;
            float row1 = 0.2f;
            float row2 = 1 - row1;
            ImGui.setNextWindowPos(view.x, view.y, ImGuiCond.Always);
            ImGui.setNextWindowSize(view.width * col1, view.height, ImGuiCond.Always);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 10f);
            nodeCanvas.render();
            ImGui.popStyleVar();

            ImGui.setNextWindowPos(view.x + view.width * col1, view.y + view.height * row1, ImGuiCond.Always);
            ImGui.setNextWindowSize(view.width * col2, view.height * row2, ImGuiCond.Always);
            ImGui.showMetricsWindow();

            ImGui.setNextWindowPos(view.x + view.width * col1, view.y, ImGuiCond.Always);
            ImGui.setNextWindowSize(view.width * col2, view.height * row1, ImGuiCond.Always);
            ImGui.begin("Hello, gui!");
            ImGui.text("Maybe this will have 20 percent fewer headaches than scene2d");
            if (ImGui.button(STR."\{FontAwesomeIcons.MousePointer} Click me!")) {
                Gdx.app.log("ImGui", "Button clicked!");
            }
            ImGui.end();

//            ImGui.showDemoWindow();
        }
        imgui.endFrame();
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        gdx.dispose();
        pixel.dispose();
        atlas.dispose();
        VisUI.dispose();
        canvasImNodes.dispose();
        canvasNodeEditor.dispose();
        imgui.dispose();
    }

    private void setActiveNodeCanvas(NodeCanvas.Type nodeCanvasType) {
        this.nodeCanvasType = nodeCanvasType;
        this.nodeCanvas = switch (nodeCanvasType) {
            case IM_NODES -> canvasImNodes;
            case NODE_EDITOR -> canvasNodeEditor;
        };
    }
}
