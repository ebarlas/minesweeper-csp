## Minesweeper CSP

Minesweeper CSP is a Java implementation of [Microsoft Minesweeper](https://en.wikipedia.org/wiki/Microsoft_Minesweeper)
using [communicating sequential processes (CSP)](https://en.wikipedia.org/wiki/Communicating_sequential_processes).

Each game tile is an independent, anonymous process and _all_ communication occurs via (buffered) channels.
No explicit synchronization primitives are used beyond channels.

It's built atop virtual threads, defined in [JDK Enhancement Proposal (JEP) 425](https://openjdk.java.net/jeps/425)
and available as a preview feature starting in Java 19.

The virtual threads feature is part of [Project Loom](https://openjdk.java.net/projects/loom/).

Prior to virtual threads, CSP-style programming in this manner simply wasn't available in Java.

![Channels](docs/minesweeper-channels.png)

# Build

An [OpenJDK 19](https://jdk.java.net/19/) or later build is required. At the time of this writing, virtual
threads are a preview feature. That is why `--enable-preview` is provided below.

---
Build with `mvn`:
```shell
mvn compile
```

Run:
```shell
java --enable-preview -cp target/classes/ minesweeper.Main
```

---
Compile with `javac`:
```shell
javac --enable-preview -source 19 src/main/java/minesweeper/*.java -d build/
```

Run:
```shell
java --enable-preview -cp build/ minesweeper.Main
```

## Command Line Arguments

Include `beginner`, `intermediate`, or `advanced` command-line argument to select a difficulty.

```shell
java --enable-preview -cp build/ minesweeper.Main advanced
```

| Difficulty   | Rows | Columns | Mines |
|--------------| ---- |---------|-------|
| Beginner     | 9    | 9       | 10    |
| Intermediate | 16   | 16      | 40    |
| Advanced     | 16   | 30      | 99    |

## Assets

All game assets were created from scratch in [Inkscape](https://inkscape.org/) and rasterized to PNG images.

![](images/digit_0.png) ![](images/digit_1.png) ![](images/digit_2.png) ![](images/digit_3.png) ![](images/digit_4.png) ![](images/digit_5.png) ![](images/digit_6.png) ![](images/digit_7.png) ![](images/digit_8.png) ![](images/digit_9.png)

![](images/tile_0.png) ![](images/tile_1.png) ![](images/tile_2.png) ![](images/tile_3.png) ![](images/tile_4.png) ![](images/tile_5.png) ![](images/tile_6.png) ![](images/tile_7.png) ![](images/tile_8.png) ![](images/tile_flag.png) ![](images/tile_mine.png) ![](images/tile.png)

![](images/face_playing.png) ![](images/face_win.png) ![](images/face_lose.png)

![](images/background_large.png) ![](images/background_medium.png) ![](images/background_small.png)

## Processes

Every tile runs in its own process, defined in [Cell.java](src/main/java/minesweeper/Cell.java).
Cell processes communicate with each other via channels.

The game controller runs in its own process, defined in [Game.java](src/main/java/minesweeper/Game.java).

The window runs in its own process, defined in [Window.java](src/main/java/minesweeper/Window.java).

Finally, the clock runs in its own process, defined in [Clock.java](src/main/java/minesweeper/Clock.java).