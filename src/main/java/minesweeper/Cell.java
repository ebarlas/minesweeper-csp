package minesweeper;

import java.util.List;

class Cell {

    private final int row;
    private final int col;
    private final boolean mine;
    private final int neighborMines;

    private final Channel<CellSignal> inChannel;
    private final List<Channel<CellSignal>> outChannels;
    private final Channel<GameSignal> stateChannel;

    private boolean revealed;
    private boolean flagged;
    private int neighborFlags;

    Cell(
            int row,
            int col,
            boolean mine,
            int neighborMines,
            Channel<CellSignal> inChannel,
            List<Channel<CellSignal>> outChannels,
            Channel<GameSignal> stateChannel) {
        this.row = row;
        this.col = col;
        this.mine = mine;
        this.neighborMines = neighborMines;
        this.inChannel = inChannel;
        this.outChannels = outChannels;
        this.stateChannel = stateChannel;
    }

    void start() {
        Thread.ofVirtual().start(this::run);
    }

    private void run() {
        while (true) {
            switch (inChannel.take()) {
                case LEFT_CLICK -> {
                    if (revealed && neighborMines == neighborFlags) {
                        revealNeighbors();
                    } else {
                        revealMe();
                    }
                }
                case RIGHT_CLICK -> {
                    if (revealed && neighborMines == neighborFlags) {
                        revealNeighbors();
                    } else if (!revealed) {
                        flagged = !flagged;
                        tellNeighborsAboutFlag();
                    }
                }
                case NEIGHBOR_REVEAL -> revealMe();
                case NEIGHBOR_FLAG_SET -> neighborFlags++;
                case NEIGHBOR_FLAG_UNSET -> neighborFlags--;
                case STOP -> {
                    return;
                }
            }
            sendGameSignal();
        }
    }

    private void sendGameSignal() {
        stateChannel.put(new GameSignal.StateChange(new CellState(row, col, mine, revealed, flagged, neighborMines)));
    }

    private void revealMe() {
        if (!flagged && !revealed) {
            revealed = true;
            if (!mine && neighborMines == 0) {
                revealNeighbors();
            }
        }
    }

    private void revealNeighbors() {
        tellNeighbors(CellSignal.NEIGHBOR_REVEAL);
    }

    private void tellNeighborsAboutFlag() {
        var signal = flagged
                ? CellSignal.NEIGHBOR_FLAG_SET
                : CellSignal.NEIGHBOR_FLAG_UNSET;
        tellNeighbors(signal);
    }

    private void tellNeighbors(CellSignal signal) {
        for (var ch : outChannels) {
            ch.put(signal);
        }
    }
}
