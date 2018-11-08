package cf.terminator.tiquality.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PausableThreadPoolExecutor extends ThreadPoolExecutor {

    private final AtomicBoolean IS_PAUSED = new AtomicBoolean(false);
    private final AtomicInteger RUNNING_THREADS = new AtomicInteger(0);

    public PausableThreadPoolExecutor(int size) {
        super(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * Wait for all threads to finish their work
     * @throws InterruptedException
     */
    public void finish() throws InterruptedException{
        synchronized (RUNNING_THREADS){
            while(RUNNING_THREADS.get() > 0){
                RUNNING_THREADS.wait();
            }
        }
    }

    /**
     * Block future scheduled actions, and wait for currently running ones to exit.
     */
    public void pause() throws InterruptedException {
        IS_PAUSED.set(true);
        finish();
    }

    /**
     * Resume all scheduled actions
     */
    public void resume(){
        synchronized (IS_PAUSED){
            IS_PAUSED.set(false);
            IS_PAUSED.notifyAll();
        }
    }

    /**
     * Shuts down everything, and waits until all threads have finished their work.
     * @throws InterruptedException
     */
    public void exitFully() throws InterruptedException{
        resume();
        shutdown();
        awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }


    /**
     * Get the amount of running threads
     * @return the amount of running threads
     */
    public int getRunningThreadCount(){
        return RUNNING_THREADS.get();
    }

    /**
     * Makes sure new threads wait before executing.
     * @param thread t
     * @param runnable r
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable){
        try {
            super.beforeExecute(thread, runnable);
            synchronized (IS_PAUSED) {
                if(IS_PAUSED.get()){
                    IS_PAUSED.wait();
                }
            }
            RUNNING_THREADS.incrementAndGet();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * After a thread has executed, we make sure to decrease the RUNNING_THREADS number by one.
     * @param r r
     * @param t t
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        synchronized (RUNNING_THREADS) {
            RUNNING_THREADS.decrementAndGet();
            RUNNING_THREADS.notifyAll();
        }
    }

}
