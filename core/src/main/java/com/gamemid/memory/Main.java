package com.gamemid.memory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.gamemid.memory.entity.Player;
import com.gamemid.memory.level.ObservationSwitch;
import com.gamemid.memory.level.Block;
import com.gamemid.memory.render.WorldRenderer;
import com.gamemid.memory.world.GameWorld;
import java.util.Locale;
import java.util.Arrays;

/** Lecture flow: create resources, collect input, update World, then draw. */
public final class Main extends ApplicationAdapter {
    public static final String GAME_TITLE = "Jaguar Block";
    private static final float VIEW_WIDTH = 1280, VIEW_HEIGHT = 720;
    private static final Color MENU_BACKGROUND = Color.valueOf("0e1116");
    private static final Color LIGHT_BLUE_BACKGROUND = Color.valueOf("b4d9ef");
    private static final Color LIGHT_RED_BACKGROUND = Color.valueOf("f3c9c6");
    private static final Color LIGHT_GREEN_BACKGROUND = Color.valueOf("dcedb8");
    private static final Color DARK_BLUE_BACKGROUND = Color.valueOf("162c4a");
    private static final Color DARK_RED_BACKGROUND = Color.valueOf("451c26");
    private static final Color DARK_GREEN_BACKGROUND = Color.valueOf("18392b");
    private static final Color ICON_STONE = Color.valueOf("b6c1c9");
    private static final Color ICON_SHADOW = Color.valueOf("647484");
    private static final Color ICON_FLASH = Color.valueOf("f4d476");
    private static final float RESOURCE_ROW_X = 1088;
    private static final float RESOURCE_SPACING = 44;
    private SpriteBatch batch;
    private ShapeRenderer uiShapes;
    private BitmapFont font;
    private FreeTypeFontGenerator fontGenerator;
    private Texture playerSheet;
    private Texture leopardFace;
    private TextureRegion[] walkFrames;
    private WorldRenderer renderer;
    private Music music;
    private Music forwardMusic, backwardMusic;
    private Sound reversalWarning;
    private boolean musicReversed, warningPlayed;
    private OrthographicCamera camera;
    private FitViewport viewport, hudViewport;
    private GameWorld world;
    private GameState state = GameState.STAGE_SELECT;
    private final Rectangle[] stageButtons = {
            new Rectangle(90, 362, 530, 90), new Rectangle(90, 250, 530, 90), new Rectangle(90, 138, 530, 90)};
    private final Rectangle labButton = new Rectangle(835, 130, 320, 52);
    private final Rectangle[] pauseButtons = {
            new Rectangle(420, 350, 440, 58), new Rectangle(420, 270, 440, 58), new Rectangle(420, 190, 440, 58)};
    private int pauseSelection;
    private final Vector2 pointer = new Vector2();
    private final GlyphLayout layout = new GlyphLayout();
    private int selectedStage;
    private final boolean[] clearedStages = new boolean[3];
    private int knownRespawn;
    private float checkpointGuideRemaining;
    private boolean muted, showDebug;

    @Override public void create() {
        batch = new SpriteBatch(); uiShapes = new ShapeRenderer();
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NanumGothic-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.incremental = true;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "어둠의기억스테이지선택시작관측일시정지계속하기홈으로다시도전준비중파랑안전사망섬광탄출구";
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        font = fontGenerator.generateFont(parameter);
        renderer = new WorldRenderer();
        playerSheet = new Texture(Gdx.files.internal("characters/leopard-walk-8frames.png"));
        playerSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        leopardFace = new Texture(Gdx.files.internal("characters/leopard-face-hud.png"), true);
        leopardFace.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        TextureRegion[][] cells = TextureRegion.split(playerSheet,
                playerSheet.getWidth() / Player.WALK_COLUMNS, playerSheet.getHeight() / Player.WALK_ROWS);
        walkFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) { walkFrames[i] = cells[i / 4][i % 4]; }
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hudViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT);
        camera.position.set(VIEW_WIDTH / 2, VIEW_HEIGHT / 2, 0);
        forwardMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/glass-orchard-20s.wav"));
        backwardMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/glass-orchard-20s-reversed.wav"));
        forwardMusic.setLooping(true); backwardMusic.setLooping(true);
        forwardMusic.setVolume(0.18f); backwardMusic.setVolume(0.18f);
        reversalWarning = Gdx.audio.newSound(Gdx.files.internal("audio/reversal-warning.wav"));
        music = forwardMusic; music.play();
    }

    @Override public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        input();
        if (state == GameState.RUNNING) { logic(delta); }
        draw();
    }

    private void input() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            muted = !muted;
            forwardMusic.setVolume(muted ? 0 : 0.18f);
            backwardMusic.setVolume(muted ? 0 : 0.18f);
        }
        if (state == GameState.STAGE_SELECT) { menuInput(); return; }
        if (state == GameState.ALL_CLEARED) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { restartAllStages(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) { openMenu(); }
            return;
        }
        if (state == GameState.CLEARED || state == GameState.TIMED_OUT || state == GameState.OUT_OF_LIVES) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) { openMenu(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { startGame(world.getStageNumber()); }
            else if (state == GameState.CLEARED && Gdx.input.isKeyJustPressed(Input.Keys.N)
                    && world.getStageNumber() < 3) { startGame(world.getStageNumber() + 1); }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = state == GameState.PAUSED ? GameState.RUNNING : GameState.PAUSED;
            pauseSelection = 0;
            if (state == GameState.PAUSED) { music.pause(); } else { music.play(); }
        }
        if (state == GameState.PAUSED) {
            pauseInput();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) { showDebug = !showDebug; }
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        if (left && !right) { world.onPlayerLeft(); }
        else if (right && !left) { world.onPlayerRight(); }
        else { world.onPlayerStop(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { world.onPlayerJump(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) { world.throwFlash(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { startGame(world.getStageNumber()); }
    }

    private void menuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) { selectedStage = (selectedStage + 2) % 3; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { selectedStage = (selectedStage + 1) % 3; }
        for (int i = 0; i < 3; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) { selectedStage = i; activateSelection(); return; }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) { startGame(0); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateSelection(); return;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            pointer.set(Gdx.input.getX(), Gdx.input.getY()); hudViewport.unproject(pointer);
            for (int i = 0; i < stageButtons.length; i++) {
                if (stageButtons[i].contains(pointer)) { selectedStage = i; activateSelection(); return; }
            }
            if (labButton.contains(pointer)) { startGame(0); }
        }
    }

    private void activateSelection() {
        startGame(selectedStage + 1);
    }

    private void pauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.H) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) { openMenu(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { startGame(world.getStageNumber()); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) { pauseSelection = (pauseSelection + 2) % 3; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { pauseSelection = (pauseSelection + 1) % 3; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { activatePauseSelection(); return; }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            pointer.set(Gdx.input.getX(), Gdx.input.getY()); hudViewport.unproject(pointer);
            for (int i = 0; i < pauseButtons.length; i++) {
                if (pauseButtons[i].contains(pointer)) { pauseSelection = i; activatePauseSelection(); return; }
            }
        }
    }

    private void activatePauseSelection() {
        if (pauseSelection == 0) { state = GameState.RUNNING; music.play(); }
        else if (pauseSelection == 1) { startGame(world.getStageNumber()); }
        else { openMenu(); }
    }

    private void startGame(int stageNumber) {
        world = new GameWorld(walkFrames, stageNumber);
        if (stageNumber > 0) { selectedStage = stageNumber - 1; }
        state = GameState.RUNNING; knownRespawn = -1; checkpointGuideRemaining = 0;
        camera.zoom = 1; camera.position.set(640, 360, 0); camera.update();
        forwardMusic.stop(); backwardMusic.stop();
        music = forwardMusic; musicReversed = false; warningPlayed = false; music.play();
    }
    private void restartAllStages() {
        Arrays.fill(clearedStages, false);
        startGame(1);
    }

    private void openMenu() {
        state = GameState.STAGE_SELECT; world = null;
        setMusicDirection(false); music.play();
    }

    private void setMusicDirection(boolean reversed) {
        if (musicReversed == reversed) { return; }
        float mirroredPosition = (20 - music.getPosition()) % 20;
        music.stop(); music = reversed ? backwardMusic : forwardMusic;
        musicReversed = reversed; music.play(); music.setPosition(mirroredPosition);
    }

    private void logic(float delta) {
        Block tutorialCheckpoint = world.isStageOne() ? world.getLevel().getSections().first().getDestination() : null;
        boolean hadCheckpoint = world.isStageOne() && world.isCheckpointActive(tutorialCheckpoint);
        world.update(delta);
        if (world.isStageOne() && !hadCheckpoint && world.isCheckpointActive(tutorialCheckpoint)) {
            checkpointGuideRemaining = 6f;
        } else if (checkpointGuideRemaining > 0) {
            checkpointGuideRemaining -= delta;
        }
        if (world.isTimedOut() || world.isOutOfLives()) {
            state = world.isOutOfLives() ? GameState.OUT_OF_LIVES : GameState.TIMED_OUT;
            setMusicDirection(false);
            return;
        }
        setMusicDirection(world.isControlsReversed());
        boolean warning = world.isReversalWarning();
        if (warning && !warningPlayed && !muted) { reversalWarning.play(0.15f); }
        warningPlayed = warning;
        float normalX = MathUtils.clamp(world.getPlayer().getPosition().x + Player.WIDTH / 2,
                VIEW_WIDTH / 2, world.getLevel().getWidth() - VIEW_WIDTH / 2);
        if (knownRespawn != world.getRespawnSerial()) {
            knownRespawn = world.getRespawnSerial(); camera.position.set(normalX, 360, 0); camera.zoom = 1;
        }
        float targetX = normalX, targetY = 360, zoom = 1;
        ObservationSwitch station = world.getObservation();
        if (station != null) {
            Rectangle area = station.getView(); float weight = world.getObservationWeight();
            targetX = MathUtils.lerp(normalX, area.x + area.width / 2, weight);
            targetY = MathUtils.lerp(360, area.y + area.height / 2, weight);
            zoom = MathUtils.lerp(1, Math.max(1, area.width / VIEW_WIDTH), weight);
        }
        float follow = 1 - (float) Math.exp(-8 * delta);
        camera.position.x = MathUtils.lerp(camera.position.x, targetX, follow);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, follow);
        camera.zoom = MathUtils.lerp(camera.zoom, zoom, follow);
        camera.update();
        if (world.isCleared()) {
            clearedStages[world.getStageNumber() - 1] = true;
            state = clearedStages[0] && clearedStages[1] && clearedStages[2]
                    ? GameState.ALL_CLEARED : GameState.CLEARED;
        }
    }

    private void draw() {
        Color background = MENU_BACKGROUND;
        if (state == GameState.RUNNING) {
            boolean reversed = world.isControlsReversed();
            switch (world.getSafeTone()) {
                case RED: background = reversed ? DARK_RED_BACKGROUND : LIGHT_RED_BACKGROUND; break;
                case GREEN: background = reversed ? DARK_GREEN_BACKGROUND : LIGHT_GREEN_BACKGROUND; break;
                default: background = reversed ? DARK_BLUE_BACKGROUND : LIGHT_BLUE_BACKGROUND; break;
            }
        }
        ScreenUtils.clear(background);
        if (state == GameState.STAGE_SELECT) { drawMenu(); return; }
        if (state == GameState.PAUSED) { drawPause(); return; }
        if (state == GameState.ALL_CLEARED) { drawAllCleared(); return; }
        if (state == GameState.CLEARED || state == GameState.TIMED_OUT || state == GameState.OUT_OF_LIVES) { drawResult(); return; }
        viewport.apply(); renderer.draw(world, camera, batch, font, showDebug); hud();
    }

    private void prepareUi() {
        hudViewport.apply(); uiShapes.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
    }

    private void drawMenu() {
        prepareUi(); uiShapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 3; i++) {
            Rectangle r = stageButtons[i];
            uiShapes.setColor(i == selectedStage ? Color.valueOf("283c47") : Color.valueOf("171f29"));
            uiShapes.rect(r.x, r.y, r.width, r.height);
            uiShapes.setColor(i == 0 ? Color.valueOf("b7c7ac") : i == 1 ? Color.valueOf("c39fb7") : Color.valueOf("d4bf7c"));
            uiShapes.rect(r.x, r.y, 5, r.height);
        }
        uiShapes.setColor(Color.valueOf("202b35")); uiShapes.rect(labButton.x, labButton.y, labButton.width, labButton.height);
        uiShapes.setColor(Color.valueOf("3d444a")); uiShapes.rect(790, 220, 390, 20); uiShapes.end();
        batch.begin();
        text(GAME_TITLE, 90, 635, 2.6f, Color.valueOf("ede9de"));
        text("발판의 색을 관측하고 기억해 길을 건너세요.", 92, 590, 1.2f, Color.LIGHT_GRAY);
        for (int i = 0; i < 3; i++) {
            Rectangle r = stageButtons[i];
            text((i + 1) + "스테이지", r.x + 25, r.y + 55, 1.65f, Color.WHITE);
            if (clearedStages[i]) { text("클리어", r.x + r.width - 115, r.y + 53, 1.25f, Color.valueOf("b7d6a5")); }
        }
        batch.setColor(Color.WHITE); batch.draw(walkFrames[2], 785, 150, 400, 400);
        text("이동 연습  [T]", 870, 165, 1.2f, Color.LIGHT_GRAY);
        text("스테이지를 선택해 시작하세요.", 92, 100, 1.1f, Color.valueOf("dec58b"));
        text("클릭 또는 방향키와 Enter로 선택  ·  [1/2/3] 스테이지  ·  [M] 음소거", 92, 59, 1, Color.GRAY);
        batch.end();
    }

    private void hud() {
        prepareUi(); Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        uiShapes.begin(ShapeRenderer.ShapeType.Filled);
        if (world.isDead()) { uiShapes.setColor(0.1f, 0.02f, 0.03f, 0.35f); uiShapes.rect(0, 0, 1280, 620); }
        if (world.isAdventure()) {
            drawResourceIcons();
            drawControlBar();
        }
        uiShapes.end(); Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();
        boolean reversed = world.isControlsReversed();
        Color hudText = WorldRenderer.labelColor(reversed);
        text(world.isAdventure() ? world.getStageNumber() + "스테이지" : "이동 연습", 28, 702, 1.25f, hudText);
        float timeLeft = world.getRemainingTime();
        String timer = "연습 모드";
        if (world.isStageOne()) { timer = "남은 시간 999초"; }
        else if (world.isAdventure()) { timer = "남은 시간 " + formatTime(timeLeft); }
        centered(timer, 640, 680, 2.55f, world.getTimeLimit() > 0 && timeLeft <= 6
                        ? Color.valueOf(reversed ? "ff9a83" : "7b2030") : hudText);
        if (world.isStageOne()) {
            text("이후 스테이지에서 바가 가득 차면", 794, 674, 1.23f, hudText);
            text("좌우 키 입력이 반전됩니다.", 794, 647, 1.23f, hudText);
        }
        if (world.isAdventure()) {
            batch.setColor(Color.WHITE);
            for (int i = 0; i < world.getRemainingLives(); i++) {
                batch.draw(leopardFace, RESOURCE_ROW_X + i * RESOURCE_SPACING, 675, 44, 44);
            }
        }
        String controls = "R 다시하기  ·  Esc 일시정지";
        if (world.isAdventure()) { controls = "F 섬광탄  ·  " + controls; }
        if (world.isStageOne() || !world.isAdventure()) {
            text("A/D 또는 ←/→ 이동  ·  Space 점프", 28, 674, 1, hudText);
            text(controls, 28, 652, 1, hudText);
        } else {
            text(controls, 28, 674, 1, hudText);
        }
        if (world.isStageOne()) {
            drawStageOneTutorial(hudText);
        }
        if (showDebug) {
            text("걷기 프레임 " + (world.getPlayer().getWalkFrameIndex() + 1) + "/8  "
                    + (world.getPlayer().isGrounded() ? "착지" : "공중") + "  [F3]", 28, 566, 1, Color.GREEN);
        }
        batch.end();
    }

    private void drawStageOneTutorial(Color color) {
        ObservationSwitch first = world.getLevel().getSwitches().first();
        float playerX = world.getPlayer().getPosition().x + Player.WIDTH / 2;
        String guide;
        String detail = null;
        Rectangle pedestal = world.getLevel().getStartPlatform().getBounds();
        float approachStart = (pedestal.x + pedestal.width + first.getX()) / 2;
        if (checkpointGuideRemaining > 0) {
            guide = "깃발에 저장했습니다. 죽으면 여기서 부활하며, 첫 저장 때 목숨 1개를 회복합니다.";
            detail = "안전한 색과 같은 색의 깃발이 저장 지점입니다.";
        } else if (world.isCheckpointActive(world.getLevel().getSections().first().getDestination())) {
            return;
        } else if (playerX < approachStart) {
            guide = "A/D 또는 ←/→로 이동하고 Space로 점프하세요.";
        } else if (first.isPressed()) {
            guide = "파란 발판만 안전합니다. 관측판을 벗어나기 전에 위치를 외우세요.";
        } else if (playerX <= first.getX() + ObservationSwitch.WIDTH) {
            guide = "관측판을 밟으면 앞쪽 발판의 색을 볼 수 있습니다.";
            detail = "안전한 색과 같은 색의 깃발이 저장 지점입니다.";
        } else {
            guide = "F로 섬광탄을 던지면 앞쪽 발판의 색이 잠깐 보입니다.";
            detail = "묘비가 남아 있는 발판은 색이 계속 보입니다.";
        }
        centered(guide, 640, 600, 1.45f, color);
        if (detail != null) { centered(detail, 640, 564, 1.45f, color); }
    }

    private void drawControlBar() {
        int slots = GameWorld.CONTROL_PHASE_SECONDS;
        int remaining = world.hasControlReversal() ? MathUtils.ceil(world.getSecondsToControlChange()) : slots;
        boolean reversed = world.isControlsReversed();
        int filled = reversed ? remaining : slots - remaining;
        Color active = WorldRenderer.labelColor(reversed);
        float barWidth = slots * 14 - 3;
        float barLeft = (VIEW_WIDTH - barWidth) / 2;
        for (int i = 0; i < slots; i++) {
            uiShapes.setColor(active.r, active.g, active.b, i < filled ? 1f : 0.22f);
            uiShapes.rect(barLeft + i * 14, 625, 11, 7);
        }
        if (world.isStageOne()) {
            float arrowTip = barLeft + barWidth + 8;
            float barCenterY = 625 + 7 / 2f;
            uiShapes.setColor(active);
            uiShapes.triangle(arrowTip, barCenterY, arrowTip + 9, barCenterY + 5, arrowTip + 9, barCenterY - 5);
            uiShapes.rect(arrowTip + 9, barCenterY - 1.5f, 18, 3);
        }
    }

    private void drawResourceIcons() {
        for (int i = 0; i < world.getRemainingGraves(); i++) {
            drawGraveIcon(RESOURCE_ROW_X + i * RESOURCE_SPACING + 22, 643);
        }
        for (int i = 0; i < world.getFlashCount(); i++) {
            drawFlashIcon(RESOURCE_ROW_X + i * RESOURCE_SPACING + 22, 610);
        }
    }

    private void drawGraveIcon(float x, float y) {
        uiShapes.setColor(ICON_SHADOW);
        uiShapes.circle(x, y + 20, 11);
        uiShapes.rect(x - 11, y + 5, 22, 15);
        uiShapes.rect(x - 15, y, 30, 5);
        uiShapes.setColor(ICON_STONE);
        uiShapes.circle(x - 1, y + 20, 8);
        uiShapes.rect(x - 9, y + 7, 16, 13);
        uiShapes.rect(x - 13, y + 3, 26, 2);
        uiShapes.setColor(ICON_SHADOW);
        uiShapes.rect(x - 2, y + 11, 2, 13);
        uiShapes.rect(x - 6, y + 19, 10, 2);
    }

    private void drawFlashIcon(float x, float y) {
        uiShapes.setColor(ICON_SHADOW);
        uiShapes.rect(x - 9, y + 4, 18, 19);
        uiShapes.ellipse(x - 9, y, 18, 8);
        uiShapes.setColor(ICON_STONE);
        uiShapes.rect(x - 7, y + 6, 14, 15);
        uiShapes.ellipse(x - 9, y + 19, 18, 7);
        uiShapes.setColor(ICON_SHADOW);
        uiShapes.rect(x - 5, y + 24, 10, 5);
        uiShapes.rectLine(x + 4, y + 28, x + 11, y + 18, 3);
        uiShapes.setColor(ICON_FLASH);
        uiShapes.rect(x - 7, y + 14, 14, 5);
        uiShapes.rect(x + 9, y + 25, 7, 2);
        uiShapes.rect(x + 11.5f, y + 22.5f, 2, 7);
    }

    private void drawPause() {
        ScreenUtils.clear(Color.BLACK); prepareUi();
        uiShapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < pauseButtons.length; i++) {
            Rectangle b = pauseButtons[i];
            uiShapes.setColor(i == pauseSelection ? Color.valueOf("26363d") : Color.valueOf("151b20"));
            uiShapes.rect(b.x, b.y, b.width, b.height);
        }
        uiShapes.end(); batch.begin();
        centered("일시정지", 640, 520, 3.2f, Color.WHITE);
        String[] labels = {"계속하기  [Esc / Enter]", "처음부터 다시 시작  [R]", "홈으로 돌아가기  [H / Tab]"};
        for (int i = 0; i < pauseButtons.length; i++) {
            centered(labels[i], 640, pauseButtons[i].y + 40, 1.3f, i == pauseSelection ? Color.WHITE : Color.LIGHT_GRAY);
        }
        centered("마우스로 누르거나 위아래 방향키와 Enter로 선택하세요.", 640, 125, 1, Color.GRAY);
        batch.end();
    }

    private void drawAllCleared() {
        prepareUi(); batch.begin();
        centered("전체 클리어", 640, 500, 3.4f, Color.valueOf("dfdfbd"));
        centered("1·2·3스테이지를 모두 클리어했습니다!", 640, 420, 1.6f, Color.WHITE);
        centered("R  기록을 초기화하고 1스테이지부터 다시 시작", 640, 280, 1.4f, Color.valueOf("dec58b"));
        centered("Enter  스테이지 선택 화면으로", 640, 215, 1.2f, Color.LIGHT_GRAY);
        batch.end();
    }

    private void drawResult() {
        prepareUi(); batch.begin();
        boolean timedOut = state == GameState.TIMED_OUT;
        boolean cleared = state == GameState.CLEARED;
        centered(cleared ? world.getStageNumber() + "스테이지 클리어" : timedOut ? "시간 초과" : "목숨 소진",
                640, 500, 3, Color.valueOf(cleared ? "dfdfbd" : "ff9a83"));
        centered(cleared ? "남긴 흔적이 길이 되었습니다." : timedOut ? "제한시간 안에 출구에 도달하세요." : "R을 누르면 스테이지 처음부터 다시 시작합니다.",
                640, 440, 1.2f, Color.LIGHT_GRAY);
        centered(formatTime(world.getElapsed()), 640, 355, 3, Color.WHITE);
        centered("사망: " + world.getDeaths() + "     묘비: " + world.getMarkers().size, 640, 295, 1.4f, Color.LIGHT_GRAY);
        centered("Enter  홈으로 돌아가기       R  다시 플레이", 640, 200, 1.2f, Color.valueOf("dec58b"));
        if (cleared && world.getStageNumber() < 3) {
            centered("N  다음 스테이지", 640, 150, 1.2f, Color.LIGHT_GRAY);
        }
        batch.end();
    }

    private void text(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale * 0.62f); font.setColor(color); font.draw(batch, value, x, y);
    }
    private void centered(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale * 0.62f); layout.setText(font, value); text(value, x - layout.width / 2, y, scale, color);
    }
    public static String formatTime(float seconds) {
        int total = (int) (seconds * 100);
        return String.format(Locale.ROOT, "%02d:%02d.%02d", total / 6000, total / 100 % 60, total % 100);
    }
    @Override public void resize(int width, int height) {
        viewport.update(width, height, false); hudViewport.update(width, height, true); camera.update();
    }
    @Override public void dispose() {
        if (forwardMusic != null) { forwardMusic.dispose(); }
        if (backwardMusic != null) { backwardMusic.dispose(); }
        if (reversalWarning != null) { reversalWarning.dispose(); }
        if (renderer != null) { renderer.dispose(); }
        if (playerSheet != null) { playerSheet.dispose(); }
        if (leopardFace != null) { leopardFace.dispose(); }
        if (font != null) { font.dispose(); }
        if (fontGenerator != null) { fontGenerator.dispose(); }
        if (uiShapes != null) { uiShapes.dispose(); }
        if (batch != null) { batch.dispose(); }
    }
}
