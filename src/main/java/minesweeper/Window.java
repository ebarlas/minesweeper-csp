package minesweeper;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

class Window {

    private static final int GRID_LEFT = 15;
    private static final int GRID_TOP = 81;
    private static final int CELL_SIDE = 20;
    private static final int FACE_TOP = 18;
    private static final int FACE_SIDE = 42;
    private static final int DIGIT_PANEL_WIDTH = 65;
    private static final int DIGIT_PANEL_TOP = 21;
    private static final int DIGIT_PANEL_MARGIN = 2;
    private static final int DIGIT_WIDTH = 19;
    private static final int[] WIDTH = {210, 350, 630};
    private static final int[] HEIGHT = {276, 416, 416};
    private static final int[] FACE_LEFT = {84, 154, 273};
    private static final int[] FLAGS_PANEL_LEFT = {16, 20, 20};

    private final Images images;
    private final Canvas canvas;
    private final Mode mode;

    private final Channel<GameState> windowChannel;
    private final Channel<GameSignal> gameChannel;

    private GameState gameState;

    private record ImageLoader(String dir) {
        BufferedImage load(String file) {
            try {
                return ImageIO.read(new File(dir, file + ".png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class Images {
        final BufferedImage[] backgrounds;
        final BufferedImage[] digits;
        final BufferedImage digitPanel;
        final BufferedImage faceSad;
        final BufferedImage faceHappy;
        final BufferedImage faceCool;
        final BufferedImage tileCovered;
        final BufferedImage tileFlag;
        final BufferedImage tileMine;
        final BufferedImage[] tiles;

        Images(ImageLoader loader) {
            backgrounds = Stream.of("small", "medium", "large")
                    .map(s -> "background_" + s)
                    .map(loader::load)
                    .toArray(BufferedImage[]::new);
            digits = IntStream.range(0, 10)
                    .mapToObj(d -> loader.load("digit_%d".formatted(d)))
                    .toArray(BufferedImage[]::new);
            digitPanel = loader.load("digit_panel");
            faceSad = loader.load("face_lose");
            faceCool = loader.load("face_win");
            faceHappy = loader.load("face_playing");
            tileCovered = loader.load("tile");
            tileFlag = loader.load("tile_flag");
            tileMine = loader.load("tile_mine");
            tiles = IntStream.range(0, 9)
                    .mapToObj(d -> loader.load("tile_%d".formatted(d)))
                    .toArray(BufferedImage[]::new);
        }
    }

    Window(
            String assetsDir,
            Mode mode,
            Channel<GameState> windowChannel,
            Channel<GameSignal> gameChannel,
            GameState initialState) {
        this.windowChannel = windowChannel;
        this.gameChannel = gameChannel;
        this.gameState = initialState;
        this.mode = mode;
        images = new Images(new ImageLoader(assetsDir));
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH[mode.ordinal()], HEIGHT[mode.ordinal()]));
        canvas.addMouseListener(new MouseClickHandler());
        JFrame frame = new JFrame("Minesweeper");
        frame.add(canvas);
        frame.setVisible(true);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private class MouseClickHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (inside(GRID_LEFT, GRID_TOP, mode.columns * CELL_SIDE, mode.rows * CELL_SIDE, e.getX(), e.getY())) {
                var row = (e.getY() - GRID_TOP) / CELL_SIDE;
                var col = (e.getX() - GRID_LEFT) / CELL_SIDE;
                var click = e.getButton() == MouseEvent.BUTTON1 ? ClickSide.LEFT : ClickSide.RIGHT;
                gameChannel.put(new GameSignal.TilePress(row, col, click));
            }
            if (inside(FACE_LEFT[mode.ordinal()], FACE_TOP, FACE_SIDE, FACE_SIDE, e.getX(), e.getY())) {
                gameChannel.put(new GameSignal.FacePress());
            }
        }

        static boolean inside(int left, int top, int width, int height, int x, int y) {
            return x >= left && y >= top && x < left + width && y < top + height;
        }
    }

    void start() {
        Thread.ofVirtual().start(this::run);
    }

    private void run() {
        while (true) {
            switch (windowChannel.take()) {
                case GameState gs -> onGameState(gs);
            }
        }
    }

    private void onGameState(GameState gs) {
        EventQueue.invokeLater(() -> {
            gameState = gs;
            canvas.repaint();
        });
    }

    private void drawDigits(Graphics g, int numDigits, int right, int top, int width, int val) {
        for (int i = 0; i < numDigits; i++) {
            var digit = val % 10;
            g.drawImage(images.digits[digit], right - width, top, null);
            val /= 10;
            right -= width;
        }
    }

    private void drawBackground(Graphics g) {
        g.drawImage(images.backgrounds[mode.ordinal()], 0, 0, null);
    }

    private void drawFlagsPanel(Graphics g) {
        g.drawImage(images.digitPanel, FLAGS_PANEL_LEFT[mode.ordinal()], DIGIT_PANEL_TOP, null);
        drawDigits(
                g,
                3,
                FLAGS_PANEL_LEFT[mode.ordinal()] + DIGIT_PANEL_WIDTH,
                DIGIT_PANEL_MARGIN + DIGIT_PANEL_TOP,
                DIGIT_PANEL_MARGIN + DIGIT_WIDTH,
                Math.max(0, mode.mines - countFlags()));
    }

    private int countFlags() {
        return Stream.of(gameState.cellStates()).flatMap(Stream::of).mapToInt(c -> c.flagged() ? 1 : 0).sum();
    }

    private void drawTimePanel(Graphics g) {
        g.drawImage(
                images.digitPanel,
                WIDTH[mode.ordinal()] - FLAGS_PANEL_LEFT[mode.ordinal()] - DIGIT_PANEL_WIDTH,
                DIGIT_PANEL_TOP,
                null);
        drawDigits(
                g,
                3,
                WIDTH[mode.ordinal()] - FLAGS_PANEL_LEFT[mode.ordinal()],
                DIGIT_PANEL_MARGIN + DIGIT_PANEL_TOP,
                DIGIT_PANEL_MARGIN + DIGIT_WIDTH,
                (int) Math.min(999, gameState.time()));
    }

    private void drawFace(Graphics g) {
        var img = switch (gameState.state()) {
            case INIT -> images.faceHappy;
            case PLAYING -> images.faceHappy;
            case LOST -> images.faceSad;
            case WON -> images.faceCool;
        };
        g.drawImage(img, FACE_LEFT[mode.ordinal()], FACE_TOP, null);
    }

    private void drawTiles(Graphics g) {
        var css = gameState.cellStates();
        mode.rowCols().forEach(rc -> g.drawImage(
                tileImage(css[rc.row()][rc.col()]),
                GRID_LEFT + rc.col() * CELL_SIDE,
                GRID_TOP + rc.row() * CELL_SIDE,
                null));
    }

    private BufferedImage tileImage(CellState cs) {
        if (cs.revealed()) {
            if (cs.mine()) {
                return images.tileMine;
            } else {
                return images.tiles[cs.neighborMines()];
            }
        } else {
            if (cs.flagged()) {
                return images.tileFlag;
            } else {
                return images.tileCovered;
            }
        }
    }

    private class Canvas extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            drawBackground(g);
            drawFlagsPanel(g);
            drawTimePanel(g);
            drawFace(g);
            drawTiles(g);
        }
    }

}
