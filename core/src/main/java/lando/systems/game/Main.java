package lando.systems.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisTable;
import imgui.ImGui;
import imgui.ImGuiConfigFlags;
import imgui.ImGuiLoader;
import imgui.gdx.ImGuiGdxImpl;
import space.earlygrey.shapedrawer.ShapeDrawer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    public static Main get;

    public SpriteBatch batch;
    public ShapeDrawer shapes;
    public OrthographicCamera windowCamera;
    public InputMultiplexer inputMux;

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

    private ImGuiGdxImpl imgui;

    public Main() {
        Main.get = this;
    }

    @Override
    public void create() {
        ImGuiLoader.init(() -> {});
        ImGui.CreateContext();
        ImGui.GetIO().set_ConfigFlags(ImGuiConfigFlags.ImGuiConfigFlags_DockingEnable);
        imgui = new ImGuiGdxImpl();

        batch = new SpriteBatch();
        shapes = new ShapeDrawer(batch);
        inputMux = new InputMultiplexer();

        windowCamera = new OrthographicCamera();
        windowCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        windowCamera.update();

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
        atlas = new TextureAtlas(Gdx.files.internal(skinFile + ".atlas"));
        radioBtnTextures = new RadioButtonTextures(
            atlas.findRegion("vis-radio"),
            atlas.findRegion("vis-radio-over"),
            atlas.findRegion("vis-radio-down"),
            atlas.findRegion("vis-radio-tick"),
            atlas.findRegion("vis-radio-tick-disabled"),
            atlas.findRegion("border-circle"),
            atlas.findRegion("border-circle-error")
        );

        skin = new Skin(Gdx.files.internal(skinFile + ".json"), atlas);
        skin.addRegions(atlas);
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

        stage.act(dt);

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

        // Start a new ImGui frame
        imgui.newFrame();
        {
            ImGui.Begin("Hello, imgui!");
            if (ImGui.Button("Show Modal")) {
                ImGui.OpenPopup("Modal");
            }
            if (ImGui.BeginPopupModal("Modal")) {
                ImGui.Text("Hello from the modal!");
                if (ImGui.Button("Close")) {
                    ImGui.CloseCurrentPopup();
                }
                ImGui.EndPopup();
            }
            ImGui.End();
        }
        ImGui.Render();
        imgui.render(ImGui.GetDrawData());
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        gdx.dispose();
        pixel.dispose();
        atlas.dispose();
        VisUI.dispose();
        imgui.dispose();
    }
}
