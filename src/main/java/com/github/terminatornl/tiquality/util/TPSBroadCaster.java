package com.github.terminatornl.tiquality.util;

import com.github.terminatornl.tiquality.Tiquality;

public class TPSBroadCaster {

    /**
     * Used for debugging purposes.
     * Prints the average TPS every 2 seconds.
     */
    public static void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                        Tiquality.log_sync("TPS: " + Math.floor(Tiquality.TPS_MONITOR.getAverageTPS() * 100) / 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
