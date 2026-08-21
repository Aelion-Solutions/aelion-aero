package com.aelion.aero.common;

import java.io.Closeable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Single daemon thread for Aero panel I/O (fleet refresh, self-status).
 *
 * <p>Not a Bukkit scheduler. Tasks must not touch world/player APIs.
 */
public final class AeroIo implements Closeable {

    private final ScheduledThreadPoolExecutor executor;

    public AeroIo() {
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "aero-io");
            t.setDaemon(true);
            return t;
        });
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public ScheduledExecutorService executor() {
        return executor;
    }

    /**
     * Run {@code task} after {@code initialDelay}, then {@code period} after each completion.
     * Exceptions inside {@code task} are swallowed so the schedule keeps running.
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(
            final Runnable task,
            long initialDelay,
            long period,
            TimeUnit unit
    ) {
        return executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (RuntimeException ignored) {
                    // keep aero-io alive
                }
            }
        }, initialDelay, period, unit);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
