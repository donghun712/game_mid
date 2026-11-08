package com.gamemid.memory.entity;

import com.badlogic.gdx.math.Vector2;
import com.gamemid.memory.level.Block;
import com.badlogic.gdx.utils.Array;

public final class MemoryFlash {
    private static final float REVEAL_DISTANCE = 800f;
    private final float originX;
    private final int direction;
    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private float fuse = 0.8f;
    private float revealRemaining;
    private boolean exploded;

    public MemoryFlash(float x, float y, boolean right) {
        originX = x;
        direction = right ? 1 : -1;
        position.set(x, y);
        velocity.set(direction * 380, 330);
    }

    public void update(float delta, Array<Block> blocks) {
        if (exploded) { revealRemaining -= delta; return; }
        float remaining = delta;
        while (remaining > 0 && !exploded) {
            float step = Math.min(remaining, 1f / 120f);
            velocity.y -= 920f * step;
            position.mulAdd(velocity, step);
            fuse -= step;
            boolean hit = false;
            for (Block block : blocks) {
                if (block.getBounds().contains(position)) { hit = true; break; }
            }
            if (hit || fuse <= 0) { exploded = true; revealRemaining = 0.5f; }
            remaining -= step;
        }
    }

    public boolean reveals(Block block) {
        float nearEdge = block.getBounds().x + (direction < 0 ? block.getBounds().width : 0);
        float ahead = (nearEdge - originX) * direction;
        return exploded && revealRemaining > 0 && ahead > 0 && ahead <= REVEAL_DISTANCE;
    }
    public boolean isExpired() { return exploded && revealRemaining <= 0; }
    public boolean isExploded() { return exploded; }
    public Vector2 getPosition() { return position; }
}
