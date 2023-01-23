package minesweeper;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Game {

    private final Mode mode;
    private final Channel<GameState> windowChannel;
    private final Channel<GameSignal> gameChannel;

    private List<List<Channel<CellSignal>>> cellChannels;

    private long startNanos;
    private long elapsedSeconds;
    private PlayState playState;
    private CellState[][] cellStates;

    Game(Mode mode, Channel<GameState> windowChannel, Channel<GameSignal> gameChannel) {
        this.mode = mode;
        this.windowChannel = windowChannel;
        this.gameChannel = gameChannel;
        playState = PlayState.INIT;
        resetCells();
    }

    void start() {
        Thread.ofVirtual().start(this::run);
    }

    private void run() {
        while (true) {
            switch (gameChannel.take()) {
                case GameSignal.TilePress tp -> onTilePress(tp);
                case GameSignal.StateChange sc -> onStateChange(sc);
                case GameSignal.ClockTick ct -> onClockTick(ct);
                case GameSignal.FacePress fp -> onFacePress(fp);
            }
        }
    }

    private void onTilePress(GameSignal.TilePress tp) {
        if (tp.clickSide() == ClickSide.LEFT && playState == PlayState.INIT) {
            playState = PlayState.PLAYING;
            startNanos = System.nanoTime();
        }
        var signal = tp.clickSide() == ClickSide.LEFT
                ? CellSignal.LEFT_CLICK
                : CellSignal.RIGHT_CLICK;
        cellChannels.get(tp.row()).get(tp.col()).put(signal);
    }

    private void onStateChange(GameSignal.StateChange sc) {
        var cs = sc.cellState();
        cellStates[cs.row()][cs.col()] = cs;
        if (playState == PlayState.PLAYING) {
            if (lost()) {
                playState = PlayState.LOST;
                stopCells();
            } else if (won()) {
                playState = PlayState.WON;
                stopCells();
            }
        }
        updateWindow();
    }

    private void onClockTick(GameSignal.ClockTick ct) {
        if (playState == PlayState.PLAYING) {
            elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos);
            updateWindow();
        }
    }

    private void onFacePress(GameSignal.FacePress fp) {
        playState = PlayState.INIT;
        elapsedSeconds = 0;
        stopCells();
        resetCells();
        updateWindow();
    }

    private void updateWindow() {
        var cells = new CellState[mode.rows][mode.columns];
        mode.rowCols().forEach(rc -> cells[rc.row()][rc.col()] = cellStates[rc.row()][rc.col()]);
        windowChannel.put(new GameState(cells, playState, elapsedSeconds));
    }

    private boolean lost() {
        return Stream.of(cellStates)
                .flatMap(Stream::of)
                .anyMatch(c -> c.revealed() && c.mine());
    }

    private boolean won() {
        return Stream.of(cellStates)
                .flatMap(Stream::of)
                .filter(CellState::revealed)
                .count() == mode.nonMines();
    }

    private void stopCells() {
        cellChannels.stream().flatMap(List::stream).forEach(c -> c.put(CellSignal.STOP));
    }

    private void resetCells() {
        var mines = placeMines();

        cellChannels = IntStream.range(0, mode.rows)
                .mapToObj(r -> IntStream.range(0, mode.columns).mapToObj(c -> new Channel<CellSignal>()).toList())
                .toList();

        BiFunction<Integer, Integer, Integer> neighborMines = (r, c) ->
                mode.neighborRowCols(r, c).mapToInt(rc -> mines[rc.row()][rc.col()] ? 1 : 0).sum();

        BiFunction<Integer, Integer, List<Channel<CellSignal>>> neighborChannels = (r, c) ->
                mode.neighborRowCols(r, c).map(rc -> cellChannels.get(rc.row()).get(rc.col())).toList();

        var cells = new Cell[mode.rows][mode.columns];
        mode.rowCols().forEach(rc -> {
            var r = rc.row();
            var c = rc.col();
            cells[r][c] = new Cell(
                    r,
                    c,
                    mines[r][c],
                    neighborMines.apply(r, c),
                    cellChannels.get(r).get(c),
                    neighborChannels.apply(r, c),
                    gameChannel);
            cells[r][c].start();
        });

        cellStates = new CellState[mode.rows][mode.columns];
        mode.rowCols().forEach(rc -> {
            var r = rc.row();
            var c = rc.col();
            cellStates[r][c] = new CellState(r, c, mines[r][c], false, false, neighborMines.apply(r, c));
        });
    }

    private boolean[][] placeMines() {
        var numMines = mode.mines;
        var random = new Random();
        var mines = new boolean[mode.rows][mode.columns];
        while (numMines > 0) {
            var r = random.nextInt(mode.rows);
            var c = random.nextInt(mode.columns);
            if (!mines[r][c]) {
                mines[r][c] = true;
                numMines--;
            }
        }
        return mines;
    }

}
