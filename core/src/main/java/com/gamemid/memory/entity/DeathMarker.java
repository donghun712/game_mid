package com.gamemid.memory.entity;

import com.gamemid.memory.level.Block;

public final class DeathMarker {
    public final int key;
    public final float x;
    public final float y;
    public final Block.Tone tone;

    public DeathMarker(int key, float x, float y, Block.Tone tone) {
        this.key = key;
        this.x = x;
        this.y = y;
        this.tone = tone;
    }
}
