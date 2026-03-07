package com.iutms.engine.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe sliding-window accumulator.
 * Tracks recent readings per key (e.g. zoneId) within a configurable time window.
 * Returns the current window average after each new reading.
 *
 * <p>Uses ConcurrentHashMap for per-key deques and synchronizes on each deque
 * instance to allow concurrent access across different keys without a global lock.
 *
 * <p>A background sweeper runs every {@code windowMs} milliseconds to remove deques
 * that have gone empty (all entries expired), preventing unbounded map growth.
 * A warning is logged if the map exceeds 1000 unique keys.
 */
public class SlidingWindow {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindow.class);

    private final long windowMs;
    private final ConcurrentHashMap<String, Deque<double[]>> windows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sliding-window-sweeper");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param windowMs window size in milliseconds (e.g. 300_000 for 5 minutes)
     */
    public SlidingWindow(long windowMs) {
        this.windowMs = windowMs;
        long intervalMs = windowMs > 0 ? windowMs : 60_000L;
        sweeper.scheduleAtFixedRate(this::sweep, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Add a new reading and return the current window average for the given key.
     * Entries older than {@code windowMs} are evicted on every call.
     *
     * @param key   zone or sensor identifier
     * @param value sensor reading
     * @return average of all readings still within the window (including the new one)
     */
    public double add(String key, double value) {
        long now = System.currentTimeMillis();
        Deque<double[]> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            deque.addLast(new double[]{now, value});

            long cutoff = now - windowMs;
            while (!deque.isEmpty() && deque.peekFirst()[0] < cutoff) {
                deque.pollFirst();
            }

            return deque.stream().mapToDouble(e -> e[1]).average().orElse(value);
        }
    }

    /**
     * Periodic sweep: removes deques that are empty (all entries expired).
     * Logs a warning if the number of unique keys exceeds 1000 after sweep.
     */
    private void sweep() {
        Iterator<Map.Entry<String, Deque<double[]>>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<double[]>> entry = it.next();
            Deque<double[]> deque = entry.getValue();
            synchronized (deque) {
                if (deque.isEmpty()) {
                    it.remove();
                }
            }
        }
        int size = windows.size();
        if (size > 1000) {
            log.warn("SlidingWindow map has {} unique zone keys — possible zone ID explosion", size);
        }
    }
}
