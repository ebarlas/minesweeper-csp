package minesweeper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Channel<T> {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();

    void put(T val) {
        try {
            queue.put(val);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    T take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
