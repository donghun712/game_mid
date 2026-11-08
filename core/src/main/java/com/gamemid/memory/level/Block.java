package com.gamemid.memory.level;

import com.badlogic.gdx.math.Rectangle;

public final class Block {
    public enum Tone { NEUTRAL, BLUE, RED, GREEN }
    private final Rectangle bounds;
    private final int id;
    private final Tone tone;
    private final boolean oneWay;

    public Block(float x, float y, float width, float height) {
        this(-1, x, y, width, height, Tone.NEUTRAL);
    }

    public Block(int id, float x, float y, float width, float height, Tone tone) {
        this(id, x, y, width, height, tone, false);
    }

    public Block(int id, float x, float y, float width, float height, Tone tone, boolean oneWay) {
        bounds = new Rectangle(x, y, width, height);
        this.id = id;
        this.tone = tone;
        this.oneWay = oneWay;
    }

    public int getId() { return id; }
    public Tone getTone() { return tone; }
    public boolean isOneWay() { return oneWay; }
    public boolean isDangerous(Tone safeTone) { return tone != Tone.NEUTRAL && tone != safeTone; }

    public Rectangle getBounds() {
        return bounds;
    }
}
