package minesweeper;

import java.util.stream.IntStream;
import java.util.stream.Stream;

enum Mode {
    BEGINNER(9, 9, 10),
    INTERMEDIATE(16, 16, 40),
    ADVANCED(16, 30, 99);

    final int rows;
    final int columns;
    final int mines;

    Mode(int rows, int columns, int mines) {
        this.rows = rows;
        this.columns = columns;
        this.mines = mines;
    }

    int nonMines() {
        return rows * columns - mines;
    }

    record RowCol(int row, int col) {}

    Stream<RowCol> rowCols() {
        return IntStream.range(0, rows)
                .boxed()
                .flatMap(r -> IntStream.range(0, columns).mapToObj(c -> new RowCol(r, c)));
    }

    Stream<RowCol> neighborRowCols(int row, int col) {
        return IntStream.rangeClosed(row - 1, row + 1)
                .boxed()
                .flatMap(r -> IntStream.rangeClosed(col - 1, col + 1).mapToObj(c -> new RowCol(r, c)))
                .filter(rc -> rc.row() >= 0 && rc.row() < rows) // discard invalid rows
                .filter(rc -> rc.col() >= 0 && rc.col() < columns) // discard invalid cols
                .filter(rc -> rc.row() != row || rc.col() != col); // discard center
    }
}
