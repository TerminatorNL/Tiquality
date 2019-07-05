package cf.terminator.tiquality.util;

import cf.terminator.tiquality.Tiquality;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

public class Scheduler {

    public static final Scheduler INSTANCE = new Scheduler();
    private final ArrayList<Runnable> queue = new ArrayList<>();

    private Scheduler(){}

    /**
     * Ensures your Runnable runs in the main thread.
     * @param runnable task
     * @return task, converted to ensure it runs in the main thread.
     */
    public Runnable convertSync(Runnable runnable){
        return new Runnable() {
            @Override
            public void run() {
                Scheduler.INSTANCE.schedule(runnable);
            }
        };
    }

    /**
     * Schedules an action to run on the main thread.
     * Actions are performed in order.
     * @param runnable action to perform
     */
    public synchronized void schedule(Runnable runnable){
        queue.add(runnable);
    }

    /**
     * Performs an action to run on the main thread. Does not return a result.
     * To obtain a result, please consult: SynchronizedAction#run
     *
     * This method will BLOCK until the action is performed.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     *
     * Hazardous thread: net.minecraftforge.common.chunkio.ChunkIOProvider -- Chunk I/O Executor Thread
     *
     * @param runnable action to perform
     */
    public void scheduleWait(final Runnable runnable){
        if (FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread()){
            runnable.run();
        }else{
            schedule(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    synchronized (runnable) {
                        runnable.notify();
                    }
                }
            });
            synchronized (runnable) {
                try {
                    runnable.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    @SubscribeEvent
    public synchronized void onServerTick(TickEvent.ServerTickEvent e){
        if (queue.size() > 0){
            long maxTime = System.currentTimeMillis() + 5000;
            while(queue.size() > 0 && System.currentTimeMillis() < maxTime){
                try {
                    queue.remove(0).run();
                }catch (Throwable t){
                    Tiquality.LOGGER.fatal("Scheduled action threw an error!");
                    throw t;
                }
            }
        }
    }
}
