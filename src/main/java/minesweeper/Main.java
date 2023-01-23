package minesweeper;

import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        var mode = modeFromArgs(args);
        var cellStates = new CellState[mode.rows][mode.columns];
        mode.rowCols()
                .map(rc -> new CellState(rc.row(), rc.col(), false, false, false, 0))
                .forEach(cs -> cellStates[cs.row()][cs.col()] = cs);
        var windowChannel = new Channel<GameState>();
        var gameChannel = new Channel<GameSignal>();
        var timer = new Clock(gameChannel);
        timer.start();
        var initialState = new GameState(cellStates, PlayState.PLAYING, 0);
        var window = new Window("images", mode, windowChannel, gameChannel, initialState);
        window.start();
        var game = new Game(mode, windowChannel, gameChannel);
        game.start();
    }

    static Mode modeFromArgs(String[] args) {
        if (args.length > 0) {
            var mode = Stream.of(Mode.values())
                    .filter(m -> m.name().equalsIgnoreCase(args[0]))
                    .findFirst();
            if (mode.isPresent()) {
                return mode.get();
            }
        }
        return Mode.INTERMEDIATE;
    }

}
