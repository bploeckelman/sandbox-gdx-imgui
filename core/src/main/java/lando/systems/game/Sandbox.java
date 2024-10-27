package lando.systems.game;

import com.badlogic.gdx.Game;

public class Sandbox extends Game {

    @Override
    public void create() {
        setScreen(new LoadScreen(this));
    }
}
