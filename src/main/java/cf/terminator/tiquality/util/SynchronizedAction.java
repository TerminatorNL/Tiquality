package cf.terminator.tiquality.util;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SynchronizedAction {

    /**
     * Convenience method.
     * it will scheduleDelayedChunkUpdate the action to the main server thread.
     * This will BLOCK until the server retrieves the result. This time may be a full tick.
     * After the action is executed, it will return the result stored in the DynamicVar type 'result'
     *
     * This is useful if you need a result on the main server thread, and want to process it in another thread.
     *
     * If you don't need a return value, this isn't for you.
     *
     * @param action the action
     * @return The result stored in the DynamicVar type 'result'
     */
    public static <V> V run(Action<V> action){
        new SynchronizedAction(action);
        return action.getResult();
    }


    private final Action action;

    private SynchronizedAction(Action synchronized_action){
        this.action = synchronized_action;
        if (FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread()){
            action.run();
        }else {
            synchronized (this.action) {
                MinecraftForge.EVENT_BUS.register(this);
                try {
                    this.action.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e){
        MinecraftForge.EVENT_BUS.unregister(this);
        action.run();
        synchronized (this.action) {
            this.action.notify();
        }
    }

    public static abstract class Action<V> implements Runnable{
        private final DynamicVar<V> variable = new DynamicVar<>();

        public V getResult(){
            return this.variable.get();
        }

        @Override
        public void run() {
            run(this.variable);
        }

        public abstract void run(DynamicVar<V> variable);
    }

    public static class DynamicVar<V> {
        private V result;

        public synchronized V get(){
            return result;
        }

        public synchronized void set(V variable){
            this.result = variable;
        }
    }

}
