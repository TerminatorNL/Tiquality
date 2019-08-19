package com.github.terminatornl.tiquality.monitor;

import com.github.terminatornl.tiquality.util.Constants;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class TPSMonitor {

    public static final TPSMonitor INSTANCE = new TPSMonitor();

    private int measure_ticks = 40;
    private long LAST_TICK = System.nanoTime();
    private long[] buffer = new long[measure_ticks];
    private int currentIndex = 0;


    /* When starting the TPS monitor, we assume optimal TPS. */
    private TPSMonitor() {
        Arrays.fill(buffer, Constants.NS_IN_TICK_LONG);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public synchronized void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) {
            return;
        }
        long now = System.nanoTime();
        buffer[currentIndex] = now - LAST_TICK;
        LAST_TICK = now;

        currentIndex++;
        if (currentIndex >= measure_ticks) {
            currentIndex = 0;
        }
    }

    /**
     * Gets the average time in nanoseconds per tick.
     * This variable will fluctuate between 1 and 1000000000, but
     * values above 1000000000 are possible as well.
     *
     * @return the average time in nanoseconds.
     */
    public synchronized long getAverageTime() {
        BigInteger total = BigInteger.ZERO;
        for (long duration : buffer) {
            total = total.add(BigInteger.valueOf(duration));
        }

        return Math.max(1L, total.divide(BigInteger.valueOf(measure_ticks)).longValue());
    }

    /**
     * Gets the average TPS of the server.
     * This variable will fluctuate between 0 and 20, but
     * values above 20 are possible as well.
     *
     * @return Average TPS
     */
    public synchronized double getAverageTPS() {
        return 1000000000D / getAverageTime();
    }


    /**
     * Gets the internal buffer of the TPSMonitor.
     * It returns a copy of the stored values.
     * <p>
     * The array that is returned is NOT ordered.
     * <p>
     * The values are tick timings in nanoseconds.
     *
     * @return the buffer
     */
    public synchronized long[] getTickHistory() {
        return buffer.clone();
    }


    /**
     * Gets the last tick time in nanoseconds
     *
     * @return the last tick time in nanoseconds
     */
    public synchronized long getLastTickTime() {
        return buffer[currentIndex - 1];
    }
}
