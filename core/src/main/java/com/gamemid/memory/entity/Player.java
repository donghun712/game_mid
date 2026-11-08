package com.gamemid.memory.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public final class Player {
    public static final int WALK_COLUMNS = 4;
    public static final int WALK_ROWS = 2;
    public static final int WALK_FRAME_COUNT = WALK_COLUMNS * WALK_ROWS;
    public static final float WIDTH = 88f;
    public static final float HEIGHT = 62f;
    public static final float MOVE_SPEED = 280f;
    public static final float JUMP_SPEED = 692.51f;
    private static final float WALK_FRAME_DURATION = 0.06f;
    public static final float DISPLAY_WIDTH = 180f;
    private static final float FOOT_PADDING_RATIO = 0.225f;

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle bounds = new Rectangle(0, 0, WIDTH, HEIGHT);
    private final Animation<TextureRegion> walk;
    private final Sprite sprite;
    private boolean grounded;
    private boolean facingRight = true;
    private float animationTime;
    private int displayedFrame;

    public Player(TextureRegion[] walkFrames) {
        if (walkFrames.length != WALK_FRAME_COUNT) {
            throw new IllegalArgumentException("Walking animation requires 8 frames");
        }
        walk = new Animation<TextureRegion>(WALK_FRAME_DURATION, walkFrames);
        walk.setPlayMode(Animation.PlayMode.LOOP);
        sprite = new Sprite(walkFrames[0]);
        float displayHeight = DISPLAY_WIDTH * walkFrames[0].getRegionHeight()
                / walkFrames[0].getRegionWidth();
        sprite.setSize(DISPLAY_WIDTH, displayHeight);
    }

    public void reset(float x, float y) {
        setPosition(x, y);
        velocity.setZero();
        grounded = false;
        facingRight = true;
        animationTime = 0;
        displayedFrame = 0;
    }

    public void jump() {
        // GameWorld checks ground contact, input buffering and edge grace first.
        velocity.y = JUMP_SPEED;
        grounded = false;
    }

    public void updateAnimation(float delta, int direction) {
        if (direction != 0) {
            facingRight = direction > 0;
        }
        if (grounded && direction != 0 && delta > 0.00001f) {
            animationTime += delta;
            displayedFrame = walk.getKeyFrameIndex(animationTime);
        } else if (grounded) {
            displayedFrame = 0;
        }
        // Preserve cycle phase on brief stops and hold the current pose in the air.
        sprite.setRegion(walk.getKeyFrames()[displayedFrame]);
        sprite.setFlip(!facingRight, false);
        syncSpriteToPosition();
    }

    // Keep the lecture's separate position-to-sprite synchronization step.
    public void syncSpriteToPosition() {
        sprite.setPosition(position.x + WIDTH / 2f - sprite.getWidth() / 2f,
                position.y - sprite.getHeight() * FOOT_PADDING_RATIO);
    }

    public void draw(SpriteBatch batch) {
        sprite.draw(batch);
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
        bounds.setPosition(x, y);
    }

    public Vector2 getPosition() { return position; }
    public Vector2 getVelocity() { return velocity; }
    public Rectangle getBounds() { return bounds; }
    public boolean isGrounded() { return grounded; }
    public boolean isFacingRight() { return facingRight; }
    public int getWalkFrameIndex() { return displayedFrame; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }
}
