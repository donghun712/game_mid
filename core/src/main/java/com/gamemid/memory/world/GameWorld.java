package com.gamemid.memory.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.gamemid.memory.entity.DeathMarker;
import com.gamemid.memory.entity.MemoryFlash;
import com.gamemid.memory.entity.Player;
import com.gamemid.memory.level.Block;
import com.gamemid.memory.level.Level;
import com.gamemid.memory.level.ObservationSwitch;
import com.gamemid.memory.level.RuleGate;
import com.gamemid.memory.level.MemorySection;

/** World rules and collision calculations, following the lecture's World/Character split. */
public final class GameWorld {
    private static final float GRAVITY = -1500f;
    private static final float MAX_STEP = 1f / 120f;
    private static final float GROUND_TOLERANCE = 0.02f;
    private static final float JUMP_BUFFER = 0.12f;
    private static final float COYOTE_TIME = 0.10f;
    private static final float PLATFORM_COYOTE_TIME = 0.15f;

    private final Level level;
    private final Player player;
    private final int stageNumber;
    public static final int CONTROL_PHASE_SECONDS = 15;
    public static final float CHECKPOINT_FLAG_SCALE = 0.8f;
    private static final int[] TIME_LIMITS = {0, 0, 70, 90};
    private static final int[] FLASH_LIMITS = {0, 2, 3, 4};
    private static final int LIFE_LIMIT = 4;
    private static final int GRAVE_LIMIT = LIFE_LIMIT - 1;
    private static final int TILE_WIDTH = 264;
    private static final int TILE_HEIGHT = 20;
    private static final int COLUMN_GAP = 136;
    private static final int OBSERVATION_RUNWAY = 160;
    private static final int CHECKPOINT_EXTENSION = 344;
    private static final int RULE_GATE_OFFSET = 160;
    private static final float FLAG_SPACING = Player.DISPLAY_WIDTH * 1.5f;
    private static final int ROW_SPACING = 142;
    private Block.Tone safeTone = Block.Tone.BLUE;
    private final Array<DeathMarker> markers = new Array<DeathMarker>();
    private final Array<MemoryFlash> flashes = new Array<MemoryFlash>();
    private final Vector2 respawn = new Vector2();
    private final Vector2 lastSafePosition = new Vector2();
    private ObservationSwitch activeObservation;
    private ObservationSwitch observationView;
    private float observationWeight;
    private float elapsed;
    private float reversalElapsed;
    private float deathRemaining;
    private boolean started;
    private boolean cleared;
    private Block activeCheckpoint;
    private int deaths;
    private int remainingLives;
    private int flashCount;
    private int respawnSerial;
    private int moveDirection;
    private float jumpBufferRemaining;
    private float coyoteRemaining;
    private Block takeoffPlatform;

    public GameWorld(TextureRegion[] walkFrames, int stageNumber) {
        if (stageNumber < 0 || stageNumber > 3) { throw new IllegalArgumentException("Stage must be 0..3"); }
        this.stageNumber = stageNumber;
        player = new Player(walkFrames);
        level = stageNumber == 0 ? createTestLevel() : createGameStage();
        restart();
    }

    private Level createGameStage() {
        Level result = new Level(0, 236, 310);
        Block start = new Block(0, 140, 110, 280, 200, Block.Tone.NEUTRAL);
        result.setStartPlatform(start);
        result.addBlock(start);
        int tutorialWalkway = stageNumber == 1 ? 720 : 0;
        Block station = new Block(1, 0, 70, 924 + OBSERVATION_RUNWAY + tutorialWalkway, 40, Block.Tone.NEUTRAL, true);
        result.addColumn(station);

        // Each mask uses one bit per row, from bottom (1) to top (4).
        int[][] rows;
        int[][] safeRows;
        if (stageNumber == 1) {
            rows = new int[][] {{3, 3, 3, 3, 3}, {3, 3, 3, 3, 3}, {3, 3, 3, 3, 3}};
            safeRows = new int[][] {{2, 1, 1, 2, 1}, {1, 2, 2, 1, 2}, {2, 2, 1, 2, 2}};
        } else if (stageNumber == 2) {
            rows = new int[][] {{7, 7, 6, 6, 7, 5}, {3, 7, 5, 3, 7, 5}, {3, 7, 6, 6, 7, 3}, {6, 7, 3, 3, 7, 5}};
            safeRows = new int[][] {{1, 6, 4, 2, 3, 1}, {2, 6, 1, 2, 6, 1}, {1, 3, 4, 4, 6, 2}, {2, 5, 1, 2, 6, 1}};
        } else {
            rows = new int[][] {{7, 7, 7, 7, 7, 7, 7}, {7, 7, 7, 7, 7, 7, 7}, {7, 7, 7, 7, 7, 7, 7}};
            safeRows = new int[][] {{1, 6, 4, 4, 6, 4, 2}, {2, 5, 4, 2, 6, 1, 2}, {2, 6, 1, 2, 6, 1, 2}};
        }
        int id = 2;
        int boundaryTop = stageNumber == 1 ? 110 : 110 + ROW_SPACING;
        float checkpointOffset = stageNumber == 3
                ? RULE_GATE_OFFSET + RuleGate.WIDTH / 2 + Player.DISPLAY_WIDTH + FLAG_SPACING
                : TILE_WIDTH + CHECKPOINT_EXTENSION - 112;
        Block.Tone[] rules = {Block.Tone.BLUE, Block.Tone.RED, Block.Tone.GREEN};
        for (int section = 0; section < rows.length; section++) {
            Block.Tone safe = stageNumber == 3 ? rules[section] : Block.Tone.BLUE;
            Array<Block> memory = new Array<Block>();
            float x = station.getBounds().x + station.getBounds().width + COLUMN_GAP;
            for (int columnIndex = 0; columnIndex < rows[section].length; columnIndex++) {
                Array<Block> column = new Array<Block>();
                for (int row = 0; row < 3; row++) {
                    int bit = 1 << row;
                    if ((rows[section][columnIndex] & bit) == 0) { continue; }
                    Block.Tone tone = (safeRows[section][columnIndex] & bit) != 0
                            ? safe : otherTone(safe, section + columnIndex + row);
                    column.add(new Block(id++, x, 110 + row * ROW_SPACING - TILE_HEIGHT,
                            TILE_WIDTH, TILE_HEIGHT, tone, true));
                }
                result.addColumn(column);
                memory.addAll(column);
                x += TILE_WIDTH + COLUMN_GAP;
            }
            boolean hasCheckpoint = section + 1 < rows.length;
            int destinationWidth = hasCheckpoint ? Math.round(checkpointOffset + 112) : TILE_WIDTH;
            Block destination = new Block(id++, x, boundaryTop - TILE_HEIGHT,
                    destinationWidth, TILE_HEIGHT, Block.Tone.NEUTRAL, true);
            result.addColumn(destination);
            float buttonX = station.getBounds().x + (section == 0 ? 780 + tutorialWalkway : (TILE_WIDTH - ObservationSwitch.WIDTH) / 2f);
            float left = section == 0 ? buttonX - 124 : station.getBounds().x;
            ObservationSwitch observation = new ObservationSwitch(station, buttonX,
                    new Rectangle(left, 0, x + destinationWidth - left, 650));
            result.getSwitches().add(observation);
            result.getSections().add(new MemorySection(observation, destination, memory));
            if (section == 0) {
                result.addCheckpoint(station, buttonX - 160);
            }
            if (section + 1 == rows.length) {
                station = destination;
            } else {
                float checkpointX = x + checkpointOffset;
                result.addCheckpoint(destination, checkpointX);
                float flagX = checkpointX - FLAG_SPACING;
                float flagWidth = 28 * CHECKPOINT_FLAG_SCALE;
                Block.Tone fakeTone = stageNumber == 3 ? safe : Block.Tone.RED;
                result.getFakeCheckpoints().add(new Block(id++, flagX - flagWidth / 2, boundaryTop,
                        flagWidth, 84 * CHECKPOINT_FLAG_SCALE, fakeTone));
                if (stageNumber == 3) {
                    result.getGates().add(new RuleGate(destination, x + RULE_GATE_OFFSET, safe, rules[section + 1]));
                }
                station = new Block(id++, x + destinationWidth, boundaryTop - TILE_HEIGHT,
                        TILE_WIDTH + OBSERVATION_RUNWAY,
                        TILE_HEIGHT, Block.Tone.NEUTRAL, true);
                result.addColumn(station);
            }
        }
        Rectangle finish = station.getBounds();
        result.setGoal(new Rectangle(finish.x + 152, finish.y + finish.height, 65, 120));
        result.setWidth(finish.x + finish.width + 200);
        return result;
    }

    private Block.Tone otherTone(Block.Tone safe, int variant) {
        if (stageNumber != 3) { return Block.Tone.RED; }
        Block.Tone[] colors = {Block.Tone.BLUE, Block.Tone.RED, Block.Tone.GREEN};
        int index = safe == Block.Tone.BLUE ? 0 : safe == Block.Tone.RED ? 1 : 2;
        return colors[(index + 1 + variant % 2) % colors.length];
    }

    // The Platformer lecture creates the map inside a World method.
    private Level createTestLevel() {
        Level testLevel = new Level(180f, 110f);
        testLevel.addBlock(new Block(0, 40, 600, 70));
        testLevel.addBlock(new Block(700, 100, 220, 40));
        testLevel.addBlock(new Block(1020, 165, 220, 40));
        testLevel.addBlock(new Block(1340, 115, 260, 45));
        testLevel.addBlock(new Block(1740, 40, 820, 70));
        return testLevel;
    }

    public void onPlayerLeft() { if (canControl()) { moveDirection = -1; } }
    public void onPlayerRight() { if (canControl()) { moveDirection = 1; } }
    public void onPlayerStop() { moveDirection = 0; }

    public void onPlayerJump() {
        if (!canControl()) { return; }
        jumpBufferRemaining = JUMP_BUFFER;
        refreshGroundSupport();
        tryJump();
    }

    private boolean isPuzzleTakeoff() { return takeoffPlatform != null && takeoffPlatform.isOneWay(); }

    private static float topOf(Block block) {
        return block.getBounds().y + block.getBounds().height;
    }

    private void tryJump() {
        if (jumpBufferRemaining > 0 && (player.isGrounded() || coyoteRemaining > 0)) {
            player.jump();
            jumpBufferRemaining = 0;
            coyoteRemaining = 0;
        }
    }

    public void restart() {
        respawn.set(level.getSpawn());
        markers.clear();
        elapsed = 0;
        reversalElapsed = 0;
        deaths = 0;
        remainingLives = LIFE_LIMIT;
        started = false;
        cleared = false;
        activeCheckpoint = null;
        flashCount = getFlashLimit();
        restorePlayer();
    }

    private void restorePlayer() {
        restorePlayer(respawn);
    }

    private void restorePlayer(Vector2 position) {
        player.reset(position.x, position.y);
        lastSafePosition.set(position);
        flashes.clear();
        deathRemaining = 0;
        activeObservation = null;
        observationView = null;
        observationWeight = 0;
        takeoffPlatform = null;
        for (ObservationSwitch station : level.getSwitches()) { station.setPressed(false); }
        respawnSerial++;
        moveDirection = 0;
        safeTone = level.getSafeToneAt(player.getPosition().x + Player.WIDTH / 2);
        jumpBufferRemaining = 0;
        coyoteRemaining = 0;
        updateGroundSupport(null);
        player.updateAnimation(0, 0);
    }

    public void update(float delta) {
        float frameDelta = MathUtils.clamp(delta, 0, 0.1f);
        if (isOutOfLives()) { return; }
        if (deathRemaining > 0) {
            deathRemaining -= frameDelta;
            if (deathRemaining <= 0) { restorePlayer(); }
            return;
        }
        if (cleared || isTimedOut()) { return; }
        float previousX = player.getPosition().x;
        // Smaller collision steps prevent thin platforms being skipped on slow frames.
        float remaining = MathUtils.clamp(delta, 0f, 0.1f);
        while (remaining > 0f) {
            float step = Math.min(remaining, MAX_STEP);
            updateSafeTone();
            updatePhysics(step);
            if (deathRemaining > 0) { break; }
            if (isAdventure()) {
                if (!started && !isSupportedBy(level.getStartPlatform())) { started = true; }
                updateStageInteractions();
                if (!isClockPaused()) {
                    elapsed += step;
                    if (!isObserving()) { reversalElapsed += step; }
                    if (isTimedOut()) {
                        elapsed = getTimeLimit();
                        moveDirection = 0;
                        return;
                    }
                }
                if (cleared) { break; }
            }
            remaining -= step;
        }
        if (player.getPosition().y < -200f) {
            if (isAdventure()) {
                restorePlayer(lastSafePosition);
            } else { restart(); }
            return;
        }
        if (deathRemaining > 0) { return; }
        for (int i = flashes.size - 1; i >= 0; i--) {
            flashes.get(i).update(frameDelta, level.getBlocks());
            if (flashes.get(i).isExpired()) { flashes.removeIndex(i); }
        }
        float targetWeight = isObserving() ? 1 : 0;
        observationWeight = MathUtils.clamp(observationWeight + Math.signum(targetWeight - observationWeight)
                * Math.min(Math.abs(targetWeight - observationWeight), frameDelta / 0.35f), 0, 1);
        // Animate the distance actually moved, so a wall does not cause walking in place.
        float walkingTime = Math.abs(player.getPosition().x - previousX) / Player.MOVE_SPEED;
        int facing = moveDirection * (isControlsReversed() ? -1 : 1);
        player.updateAnimation(walkingTime, facing);
    }

    private void updatePhysics(float delta) {
        refreshGroundSupport();
        coyoteRemaining = player.isGrounded() ? coyoteTime() : Math.max(0, coyoteRemaining - delta);
        tryJump();
        Block standingOn = player.isGrounded() ? takeoffPlatform : null;
        int effectiveDirection = moveDirection * (isControlsReversed() ? -1 : 1);
        float dx = effectiveDirection * Player.MOVE_SPEED * delta;
        player.getVelocity().x = effectiveDirection * Player.MOVE_SPEED;
        player.setPosition(player.getPosition().x + dx, player.getPosition().y);
        for (Block block : level.getBlocks()) {
            if (block.isOneWay()) { continue; }
            Rectangle surface = block.getBounds();
            if (player.getBounds().overlaps(surface)) {
                if (dx > 0) {
                    player.setPosition(surface.x - Player.WIDTH, player.getPosition().y);
                } else if (dx < 0) {
                    player.setPosition(surface.x + surface.width, player.getPosition().y);
                }
            }
        }
        player.setPosition(MathUtils.clamp(player.getPosition().x, 0, level.getWidth() - Player.WIDTH),
                player.getPosition().y);

        player.getVelocity().y += GRAVITY * delta;
        float dy = player.getVelocity().y * delta;
        float previousY = player.getPosition().y;
        player.setPosition(player.getPosition().x, player.getPosition().y + dy);
        player.setGrounded(false);
        for (Block block : level.getBlocks()) {
            Rectangle surface = block.getBounds();
            if (player.getBounds().overlaps(surface)) {
                float top = surface.y + surface.height;
                // Only feet crossing the upper surface count as stepping on red.
                // Head and side collisions retain the usual solid-block response.
                if (dy < 0 && previousY >= top - GROUND_TOLERANCE && canLandOn(block)
                        && hasFootOverlap(block, block == standingOn ? 0f : 0.5f)) {
                    landOn(block);
                    if (isDead()) { return; }
                    break;
                } else if (!block.isOneWay() && dy > 0 && previousY + Player.HEIGHT <= surface.y + GROUND_TOLERANCE) {
                    player.setPosition(player.getPosition().x, surface.y - Player.HEIGHT);
                    player.getVelocity().y = 0;
                }
            }
        }
        // Exact edge contact is not an overlap. Tiny residual steps can also round
        // away the gravity displacement, so explicitly preserve surface support.
        updateGroundSupport(standingOn);
        if (player.isGrounded()) {
            coyoteRemaining = coyoteTime();
            tryJump();
        }
        jumpBufferRemaining = Math.max(0, jumpBufferRemaining - delta);
    }

    private float coyoteTime() { return isPuzzleTakeoff() ? PLATFORM_COYOTE_TIME : COYOTE_TIME; }

    private void refreshGroundSupport() {
        if (player.isGrounded() && !isSupportedBy(takeoffPlatform)) {
            takeoffPlatform = null;
            updateGroundSupport(null);
        }
    }

    private boolean canLandOn(Block block) {
        if (!isPuzzleTakeoff() || !block.isOneWay() || block == takeoffPlatform) { return true; }
        Rectangle from = takeoffPlatform.getBounds();
        Rectangle to = block.getBounds();
        float rise = topOf(block) - topOf(takeoffPlatform);
        if (from.x == to.x) { return rise == ROW_SPACING; }
        if (rise == 0 && (to.x == from.x + from.width || from.x == to.x + to.width)) { return true; }
        boolean adjacent = to.x == from.x + from.width + COLUMN_GAP
                || from.x == to.x + to.width + COLUMN_GAP;
        return adjacent && rise <= ROW_SPACING;
    }

    private void landOn(Block block) {
        player.setPosition(player.getPosition().x, topOf(block));
        if (isAdventure() && isDangerous(block)) { die(block); return; }
        takeoffPlatform = block;
        player.getVelocity().y = 0;
        player.setGrounded(true);
    }

    private boolean hasFootOverlap(Block block, float minimumFraction) {
        Rectangle feet = player.getBounds(), surface = block.getBounds();
        float overlap = Math.min(feet.x + feet.width, surface.x + surface.width) - Math.max(feet.x, surface.x);
        return overlap > 0 && overlap >= feet.width * minimumFraction;
    }

    private void updateGroundSupport(Block standingOn) {
        if (player.getVelocity().y > 0) { return; }
        Rectangle feet = player.getBounds();
        for (Block block : level.getBlocks()) {
            Rectangle surface = block.getBounds();
            float top = surface.y + surface.height;
            // Walking retains an established support; a fresh landing needs at least half of the feet.
            if (hasFootOverlap(block, block == standingOn ? 0f : 0.5f)
                    && Math.abs(feet.y - top) <= GROUND_TOLERANCE && canLandOn(block)) {
                landOn(block);
                return;
            }
        }
    }

    public Player getPlayer() { return player; }
    public Level getLevel() { return level; }

    public boolean isSupportedBy(Block block) {
        if (block == null || !player.isGrounded()) { return false; }
        Rectangle p = player.getBounds(), b = block.getBounds();
        return hasFootOverlap(block, block == takeoffPlatform ? 0f : 0.5f)
                && Math.abs(p.y - b.y - b.height) <= GROUND_TOLERANCE;
    }

    private void updateStageInteractions() {
        updateSafeTone();
        for (Block fake : level.getFakeCheckpoints()) {
            if (player.getBounds().overlaps(fake.getBounds())) {
                die(fake);
                return;
            }
        }
        for (Block block : level.getBlocks()) {
            if (!isDangerous(block) && isSupportedBy(block) && hasFootOverlap(block, 0.5f)) {
                lastSafePosition.set(player.getPosition());
            }
        }
        for (Block checkpoint : level.getCheckpoints()) {
            if (isSupportedBy(checkpoint)
                    && player.getPosition().x + Player.WIDTH / 2 >= level.getCheckpointX(checkpoint)
                    && (activeCheckpoint == null || checkpoint.getBounds().x > activeCheckpoint.getBounds().x)) {
                activeCheckpoint = checkpoint;
                Rectangle b = checkpoint.getBounds();
                respawn.set(level.getCheckpointX(checkpoint) - Player.WIDTH / 2, b.y + b.height);
                remainingLives = Math.min(LIFE_LIMIT, remainingLives + 1);
                if (!markers.isEmpty()) { markers.pop(); }
            }
        }
        if (level.getGoal().overlaps(player.getBounds())) {
            cleared = true;
            moveDirection = 0;
            return;
        }
        activeObservation = null;
        for (ObservationSwitch station : level.getSwitches()) {
            boolean pressed = station.touches(player.getBounds()) && player.isGrounded();
            station.setPressed(pressed);
            if (pressed) { activeObservation = station; }
        }
        if (activeObservation != null) {
            observationView = activeObservation;
        }
    }

    public void throwFlash() {
        if (!isAdventure() || !canControl() || flashCount <= 0) { return; }
        flashCount--;
        flashes.add(new MemoryFlash(player.getPosition().x + Player.WIDTH / 2,
                player.getPosition().y + Player.HEIGHT, player.isFacingRight()));
    }

    private void die(Block cause) {
        if (deathRemaining > 0 || cleared) { return; }
        deaths++;
        remainingLives--;
        if (level.getBlocks().contains(cause, true) && !isOutOfLives() && getRemainingGraves() > 0) {
            int key = cause.getId();
            boolean exists = false;
            for (DeathMarker marker : markers) { if (marker.key == key) { exists = true; break; } }
            if (!exists) {
                Rectangle b = cause.getBounds();
                float inset = Math.min(15, b.width / 2);
                float x = MathUtils.clamp(player.getPosition().x + Player.WIDTH / 2, b.x + inset, b.x + b.width - inset);
                markers.add(new DeathMarker(key, x, b.y + b.height, cause.getTone()));
            }
        }
        deathRemaining = 0.65f;
        activeObservation = null;
        observationView = null;
        observationWeight = 0;
        for (ObservationSwitch station : level.getSwitches()) { station.setPressed(false); }
        moveDirection = 0;
        flashes.clear();
    }

    public boolean canControl() { return deathRemaining <= 0 && !cleared && !isTimedOut() && !isOutOfLives(); }
    public boolean isStageOne() { return stageNumber == 1; }
    public boolean isAdventure() { return stageNumber > 0; }
    public int getStageNumber() { return stageNumber; }
    public Block.Tone getSafeTone() { return safeTone; }
    public boolean isDangerous(Block block) { return block.isDangerous(safeTone); }
    public boolean hasControlReversal() { return stageNumber >= 2; }
    public boolean isControlsReversed() {
        return hasControlReversal() && reversalElapsed % (2 * CONTROL_PHASE_SECONDS) >= CONTROL_PHASE_SECONDS;
    }
    public float getSecondsToControlChange() {
        if (!hasControlReversal()) { return 0; }
        return CONTROL_PHASE_SECONDS - reversalElapsed % CONTROL_PHASE_SECONDS;
    }
    public boolean isReversalWarning() { return hasControlReversal() && hasStarted() && !isTimedOut() && getSecondsToControlChange() <= 1.5f; }
    private void updateSafeTone() {
        safeTone = level.getSafeToneAt(player.getPosition().x + Player.WIDTH / 2);
    }
    public static String toneName(Block.Tone tone) {
        switch (tone) {
            case BLUE: return "파랑";
            case RED: return "빨강";
            case GREEN: return "초록";
            default: return "회색";
        }
    }
    public boolean hasStarted() { return started; }
    public boolean isOnStartPlatform() { return isAdventure() && isSupportedBy(level.getStartPlatform()); }
    public boolean isClockPaused() { return !started || isOnStartPlatform() || isDead() || cleared || isTimedOut(); }
    public float getElapsed() { return elapsed; }
    public int getTimeLimit() { return TIME_LIMITS[stageNumber]; }
    public float getRemainingTime() { return Math.max(0, getTimeLimit() - elapsed); }
    public boolean isTimedOut() { return getTimeLimit() > 0 && !cleared && elapsed >= getTimeLimit(); }
    public boolean isOutOfLives() { return isAdventure() && remainingLives <= 0; }
    public int getRemainingLives() { return remainingLives; }
    public int getRemainingGraves() { return GRAVE_LIMIT - markers.size; }
    public boolean isDead() { return deathRemaining > 0; }
    public boolean isCleared() { return cleared; }
    public boolean isCheckpointActive() { return activeCheckpoint != null; }
    public boolean isCheckpointActive(Block block) {
        return activeCheckpoint != null && block.getBounds().x <= activeCheckpoint.getBounds().x;
    }
    public int getDeaths() { return deaths; }
    public int getFlashCount() { return flashCount; }
    public int getFlashLimit() { return FLASH_LIMITS[stageNumber]; }
    public int getRespawnSerial() { return respawnSerial; }
    public Array<DeathMarker> getMarkers() { return markers; }
    public Array<MemoryFlash> getFlashes() { return flashes; }
    public boolean isObserving() { return activeObservation != null; }
    public ObservationSwitch getObservation() { return observationWeight > 0 || isObserving() ? observationView : null; }
    public float getObservationWeight() {
        return observationWeight;
    }
    public boolean isRevealed(Block block) {
        if (!isAdventure() || isObserving()) { return true; }
        for (DeathMarker marker : markers) {
            if (marker.key == block.getId()) { return true; }
        }
        for (MemoryFlash flash : flashes) {
            if (flash.reveals(block)) { return true; }
        }
        return false;
    }
}
