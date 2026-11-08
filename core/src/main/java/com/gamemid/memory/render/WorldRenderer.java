package com.gamemid.memory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.gamemid.memory.entity.DeathMarker;
import com.gamemid.memory.entity.MemoryFlash;
import com.gamemid.memory.level.Block;
import com.gamemid.memory.level.ObservationSwitch;
import com.gamemid.memory.level.RuleGate;
import com.gamemid.memory.world.GameWorld;

/** Drawing only: physics and the meaning of colors remain in GameWorld. */
public final class WorldRenderer implements Disposable {
    private static final Color SKY_TEXT = Color.valueOf("19344b");
    private static final Color REVERSED_TEXT = Color.valueOf("fff0d8");
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Texture pedestal;
    private final TextureRegion pedestalRegion;

    public WorldRenderer() {
        pedestal = new Texture(Gdx.files.internal("environment/start-pedestal.png"));
        pedestal.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        // The transparent sprite has a little padding above the stone cap.
        pedestalRegion = new TextureRegion(pedestal, 110, 55, 1550, 832);
    }

    public static Color color(Block.Tone tone) {
        switch (tone) {
            case BLUE: return Color.valueOf("499fdf");
            case RED: return Color.valueOf("e56c64");
            case GREEN: return Color.valueOf("79c653");
            default: return Color.valueOf("c2c5c4");
        }
    }

    public static Color labelColor(boolean reversed) {
        return reversed ? REVERSED_TEXT : SKY_TEXT;
    }

    public void draw(GameWorld world, OrthographicCamera camera, SpriteBatch batch,
                     BitmapFont font, boolean debug) {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Block block : world.getLevel().getBlocks()) {
            if (world.isAdventure() && block == world.getLevel().getStartPlatform()) { continue; }
            Rectangle b = block.getBounds();
            boolean neutral = block.getTone() == Block.Tone.NEUTRAL;
            Color base = neutral ? Color.valueOf("55575b")
                    : world.isRevealed(block) ? color(block.getTone()) : Color.valueOf("747d89");
            shapes.setColor(base.r * 0.55f, base.g * 0.55f, base.b * 0.55f, 1);
            shapes.rect(b.x, b.y, b.width, b.height);
            shapes.setColor(base);
            shapes.rect(b.x, b.y + b.height - Math.min(8, b.height), b.width, Math.min(8, b.height));
            shapes.setColor(base.r * 0.74f, base.g * 0.74f, base.b * 0.74f, 1);
            for (float x = b.x + 65; x < b.x + b.width; x += 75) { shapes.rect(x, b.y, 2, b.height - 8); }
        }
        if (world.isAdventure()) {
            for (RuleGate gate : world.getLevel().getGates()) {
                drawRuleGate(gate);
            }
            for (ObservationSwitch station : world.getLevel().getSwitches()) {
                shapes.setColor(station.isPressed() ? Color.valueOf("ffe1a0") : Color.valueOf("b49b68"));
                shapes.rect(station.getX(), station.getY(), 42, 6);
                shapes.circle(station.getX() + 21, station.getY() + 9, 4, 12);
            }
            for (Block checkpoint : world.getLevel().getCheckpoints()) {
                Rectangle cp = checkpoint.getBounds();
                float cx = world.getLevel().getCheckpointX(checkpoint), cy = cp.y + cp.height;
                drawCheckpointFlag(cx, cy, world.getLevel().getSafeToneAt(cx),
                        world.isCheckpointActive(checkpoint));
            }
            for (Block fake : world.getLevel().getFakeCheckpoints()) {
                Rectangle flag = fake.getBounds();
                drawCheckpointFlag(flag.x + flag.width / 2, flag.y, fake.getTone(), false);
            }
            Rectangle g = world.getLevel().getGoal();
            shapes.setColor(Color.valueOf(world.isControlsReversed() ? "a9c6bc" : "326957"));
            shapes.rect(g.x, g.y, 8, g.height); shapes.rect(g.x + g.width - 8, g.y, 8, g.height);
            shapes.rect(g.x, g.y + g.height - 8, g.width, 8);
        }
        for (DeathMarker marker : world.getMarkers()) {
            shapes.setColor(Color.valueOf("8b9298")); shapes.rect(marker.x - 9, marker.y, 18, 25);
            shapes.circle(marker.x, marker.y + 25, 9, 16); shapes.setColor(color(marker.tone));
            shapes.rect(marker.x - 4, marker.y + 13, 8, 13);
        }
        for (MemoryFlash flash : world.getFlashes()) {
            Vector2 pos = flash.getPosition(); shapes.setColor(Color.valueOf("ece2b6"));
            shapes.circle(pos.x, pos.y, flash.isExploded() ? 12 : 5, 20);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined); batch.begin();
        if (world.isAdventure()) {
            Rectangle b = world.getLevel().getStartPlatform().getBounds();
            // Match the stone cap to the collision surface; the wider base extends
            // beyond it. Pixel anchors refer to this preserved reference-image asset.
            float scale = b.width / 1125f;
            float width = pedestalRegion.getRegionWidth() * scale;
            float height = b.height + 35 * scale + 3;
            batch.draw(pedestalRegion, b.x - 215 * scale,
                    b.y + b.height + 35 * scale - height, width, height);
        }
        if (!world.isDead()) { world.getPlayer().draw(batch); }
        font.getData().setScale(0.65f);
        font.setColor(labelColor(world.isControlsReversed()));
        if (world.isAdventure()) {
            for (RuleGate gate : world.getLevel().getGates()) {
                font.draw(batch, GameWorld.toneName(gate.getBefore()) + " → " + GameWorld.toneName(gate.getAfter()),
                        gate.getX() - 46, gate.getY() + RuleGate.HEIGHT + 28);
            }
            for (ObservationSwitch station : world.getLevel().getSwitches()) {
                font.draw(batch, "관측", station.getX(), station.getY() + 36);
            }
            Rectangle g = world.getLevel().getGoal();
            font.draw(batch, "출구", g.x + 8, g.y + g.height + 28);
            Block start = world.getLevel().getStartPlatform();
            font.draw(batch, "시작 받침대", start.getBounds().x + 20, start.getBounds().y + start.getBounds().height + 130);
        } else {
            for (int i = 0; i < world.getLevel().getBlocks().size; i++) {
                Rectangle b = world.getLevel().getBlocks().get(i).getBounds();
                font.draw(batch, i == 0 ? "시작" : "점프 " + i, b.x + 12, b.y + b.height + 28);
            }
        }
        font.setColor(Color.WHITE); batch.end();
        if (debug) {
            Rectangle b = world.getPlayer().getBounds(); shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.GREEN); shapes.rect(b.x, b.y, b.width, b.height); shapes.end();
        }
    }

    private void drawRuleGate(RuleGate gate) {
        float x = gate.getX() - RuleGate.WIDTH / 2, y = gate.getY();
        float width = RuleGate.WIDTH, height = RuleGate.HEIGHT;
        shapes.setColor(Color.valueOf("34424d"));
        shapes.rect(x - 5, y, 26, 8);
        shapes.rect(x + width - 21, y, 26, 8);
        shapes.rect(x, y + 8, 16, height - 8);
        shapes.rect(x + width - 16, y + 8, 16, height - 8);
        shapes.rect(x, y + height - 24, width, 24);
        shapes.setColor(Color.valueOf("b9c5cd"));
        shapes.rect(x + 3, y + 10, 10, height - 34);
        shapes.rect(x + width - 13, y + 10, 10, height - 34);
        shapes.rect(x + 3, y + height - 5, width - 6, 3);
        shapes.setColor(color(gate.getAfter()));
        shapes.rect(x + 13, y + 12, 3, height - 38);
        shapes.rect(x + width - 16, y + 12, 3, height - 38);
        float emblemY = y + height - 14;
        shapes.setColor(color(gate.getBefore()));
        shapes.circle(gate.getX() - 31, emblemY, 7, 16);
        shapes.setColor(Color.valueOf("e8eef1"));
        shapes.rect(gate.getX() - 9, emblemY - 1.5f, 16, 3);
        shapes.triangle(gate.getX() + 7, emblemY - 5, gate.getX() + 7, emblemY + 5, gate.getX() + 13, emblemY);
        shapes.setColor(color(gate.getAfter()));
        shapes.circle(gate.getX() + 31, emblemY, 7, 16);
    }

    private void drawCheckpointFlag(float x, float y, Block.Tone tone, boolean active) {
        float scale = GameWorld.CHECKPOINT_FLAG_SCALE;
        shapes.setColor(Color.valueOf("39434c"));
        shapes.rect(x - 22 * scale, y, 44 * scale, 6 * scale);
        shapes.setColor(Color.valueOf("94a3ad"));
        shapes.rect(x - 15 * scale, y + 6 * scale, 30 * scale, 6 * scale);
        shapes.setColor(Color.valueOf("465662"));
        shapes.rect(x - 3 * scale, y + 10 * scale, 6 * scale, 74 * scale);
        shapes.setColor(Color.valueOf("d6e0e5"));
        shapes.rect(x - scale, y + 12 * scale, 2 * scale, 72 * scale);
        shapes.setColor(active ? Color.valueOf("f7d77a") : Color.valueOf("e0e7eb"));
        shapes.circle(x, y + 87 * scale, 4 * scale, 12);
        shapes.setColor(color(tone));
        shapes.triangle(x + 3 * scale, y + 46 * scale,
                x + 3 * scale, y + 81 * scale, x + 62 * scale, y + 63.5f * scale);
    }

    @Override public void dispose() { shapes.dispose(); pedestal.dispose(); }
}
