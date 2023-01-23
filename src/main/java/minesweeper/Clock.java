package minesweeper;

class Clock {

    private static final int PERIOD_MILLIS = 100;

    private final Channel<GameSignal> channel;

    Clock(Channel<GameSignal> channel) {
        this.channel = channel;
    }

    void start() {
        Thread.ofVirtual().start(this::run);
    }

    private void run() {
        while (true) {
            try {
                Thread.sleep(PERIOD_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            channel.put(new GameSignal.ClockTick());
        }
    }

}
