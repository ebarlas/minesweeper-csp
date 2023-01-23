package minesweeper;

record CellState(
        int row,
        int col,
        boolean mine,
        boolean revealed,
        boolean flagged,
        int neighborMines) {
}
