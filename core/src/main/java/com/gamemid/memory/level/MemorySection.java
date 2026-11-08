package com.gamemid.memory.level;

import com.badlogic.gdx.utils.Array;

/** The colored choices and camera bounds for one observation. */
public final class MemorySection {
    private final ObservationSwitch observation;
    private final Block destination;
    private final Array<Block> tiles;

    public MemorySection(ObservationSwitch observation, Block destination, Array<Block> tiles) {
        this.observation = observation; this.destination = destination; this.tiles = tiles;
    }

    public ObservationSwitch getObservation() { return observation; }
    public Block getDestination() { return destination; }
    public Array<Block> getTiles() { return tiles; }
}
