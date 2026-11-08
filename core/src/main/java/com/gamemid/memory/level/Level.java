package com.gamemid.memory.level;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;

/** Stage data: platforms, observation plates, checkpoint, color gates and exit. */
public final class Level {
    public static final float WIDTH = 2560f;

    private final Array<Block> blocks = new Array<Block>();
    private final Vector2 spawn;
    private float width;
    private final Array<Array<Block>> columns = new Array<Array<Block>>();
    private final Array<ObservationSwitch> switches = new Array<ObservationSwitch>();
    private final Array<RuleGate> gates = new Array<RuleGate>();
    private final Array<Block> checkpoints = new Array<Block>();
    private final ObjectFloatMap<Block> checkpointPositions = new ObjectFloatMap<Block>();
    private final Array<Block> fakeCheckpoints = new Array<Block>();
    private final Array<MemorySection> sections = new Array<MemorySection>();
    private Block startPlatform;
    private Rectangle goal;

    public Level(float spawnX, float spawnY) {
        this(WIDTH, spawnX, spawnY);
    }

    public Level(float width, float spawnX, float spawnY) {
        this.width = width;
        spawn = new Vector2(spawnX, spawnY);
    }

    public void addBlock(Block block) { blocks.add(block); }
    public Array<Block> getBlocks() { return blocks; }
    public Vector2 getSpawn() { return spawn; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public Array<Array<Block>> getColumns() { return columns; }
    public Array<ObservationSwitch> getSwitches() { return switches; }
    public Array<RuleGate> getGates() { return gates; }
    public Array<Block> getCheckpoints() { return checkpoints; }
    public Array<Block> getFakeCheckpoints() { return fakeCheckpoints; }
    public Array<MemorySection> getSections() { return sections; }
    public float getCheckpointX(Block block) {
        Rectangle bounds = block.getBounds();
        return checkpointPositions.get(block, bounds.x + bounds.width / 2);
    }
    public void addColumn(Array<Block> column) {
        columns.add(column);
        blocks.addAll(column);
    }
    public void addColumn(Block block) { addColumn(Array.with(block)); }
    public void addCheckpoint(Block block, float x) {
        if (!checkpoints.contains(block, true)) { checkpoints.add(block); }
        checkpointPositions.put(block, x);
    }
    public Block.Tone getSafeToneAt(float x) {
        Block.Tone tone = Block.Tone.BLUE;
        for (RuleGate gate : gates) { if (x >= gate.getX()) { tone = gate.getAfter(); } }
        return tone;
    }
    public Block getStartPlatform() { return startPlatform; }
    public void setStartPlatform(Block block) { startPlatform = block; }
    public Block getCheckpoint() { return checkpoints.isEmpty() ? null : checkpoints.first(); }
    public Rectangle getGoal() { return goal; }
    public void setGoal(Rectangle goal) { this.goal = goal; }
}
