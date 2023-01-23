package minesweeper;

sealed interface GameSignal {
    record TilePress(int row, int col, ClickSide clickSide) implements GameSignal {}
    record FacePress() implements GameSignal {}
    record StateChange(CellState cellState) implements GameSignal {}
    record ClockTick() implements GameSignal {}
}
