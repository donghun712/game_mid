package com.gamemid.memory.level;

/** A safe, neutral gate separating two color-rule areas. */
public final class RuleGate {
    public static final float WIDTH = 128f;
    public static final float HEIGHT = 176f;
    private final Block platform;
    private final float x;
    private final Block.Tone before, after;

    public RuleGate(Block platform, float x, Block.Tone before, Block.Tone after) {
        this.platform = platform; this.x = x; this.before = before; this.after = after;
    }

    public float getX() { return x; }
    public float getY() { return platform.getBounds().y + platform.getBounds().height; }
    public Block.Tone getBefore() { return before; }
    public Block.Tone getAfter() { return after; }
}
