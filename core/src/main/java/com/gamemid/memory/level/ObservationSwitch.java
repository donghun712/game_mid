package com.gamemid.memory.level;

import com.badlogic.gdx.math.Rectangle;

public final class ObservationSwitch {
    public static final float WIDTH = 42;
    private final Block platform;
    private final float x;
    private final Rectangle view;
    private boolean pressed;

    public ObservationSwitch(Block platform, float x, Rectangle view) {
        this.platform = platform;
        this.x = x;
        this.view = view;
    }

    public boolean touches(Rectangle player) {
        Rectangle p = platform.getBounds();
        return player.x + player.width > x && player.x < x + WIDTH
                && Math.abs(player.y - p.y - p.height) < 1f;
    }

    public Block getPlatform() { return platform; }
    public float getX() { return x; }
    public float getY() { return platform.getBounds().y + platform.getBounds().height; }
    public Rectangle getView() { return view; }
    public boolean isPressed() { return pressed; }
    public void setPressed(boolean pressed) { this.pressed = pressed; }
}
