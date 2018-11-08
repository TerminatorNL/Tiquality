package cf.terminator.tiquality.mixinhelper;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.TrackerBase;
import cf.terminator.tiquality.util.FiFoQueue;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WorldHelper {

    private static final FiFoQueue<ScheduledAction> TASKS = new FiFoQueue<>();

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All higher)
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in the main thread!
     */
    public static void setTrackerCuboid(TiqualityWorld world, BlockPos start, BlockPos end, TrackerBase tracker, Runnable callback){
        int low_x = start.getX();
        int low_y = start.getY();
        int low_z = start.getZ();

        int high_x = end.getX();
        int high_y = end.getY();
        int high_z = end.getZ();

        synchronized (TASKS) {
            for (int x = low_x; x <= high_x + 16; x = x + 16) {
                for (int y = low_y; y <= high_y + 16; y = y + 16) {
                    for (int z = low_z; z <= high_z + 16; z = z + 16) {
                        TASKS.addToQueue(new SetTrackerTask(world, new BlockPos(x,y,z), start, end, tracker));
                    }
                }
            }
            if(callback != null) {
                TASKS.addToQueue(new CallBack(callback));
            }
            MinecraftForge.EVENT_BUS.register(SmearedAction.INSTANCE);
        }
    }

    public static int getQueuedTasks(){
        synchronized (TASKS){
            return TASKS.size();
        }
    }

    /**
     * Executes tasks in the main thread, but limits itself to 100 milliseconds.
     * Used for large tasks that must be done in the main thread.
     */
    public static class SmearedAction {

        public static final SmearedAction INSTANCE = new SmearedAction();

        private SmearedAction() {

        }

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            try {

                ExecutorService threadPool = Executors.newFixedThreadPool(20);
                long maxTime = System.currentTimeMillis() + 100;
                synchronized (TASKS) {
                    while (System.currentTimeMillis() < maxTime) {
                        if (TASKS.size() == 0) {
                            MinecraftForge.EVENT_BUS.unregister(this);
                            return;
                        }
                        ScheduledAction action = TASKS.take();
                        if(action.requiresChunkLoad()){
                            action.loadChunk();
                        }
                        if (action.isCallback() == false) {
                            /* It's a task, we execute it straight away in the threadpool. */
                            threadPool.submit(action);
                        } else {
                            /* It's a callback, we wait for all Tasks to end, and then call it. */
                            threadPool.shutdown();
                            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                            action.run();
                            if(TASKS.size() == 0){
                                MinecraftForge.EVENT_BUS.unregister(this);
                                return;
                            }
                            threadPool = Executors.newFixedThreadPool(20);
                        }
                    }
                }
                threadPool.shutdown();
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    interface ScheduledAction extends Runnable{
        boolean isCallback();
        boolean requiresChunkLoad();
        void loadChunk();
    }

    public static class CallBack implements ScheduledAction{

        private final Runnable runnable;

        public CallBack(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }

        @Override
        public boolean isCallback() {
            return true;
        }

        @Override
        public boolean requiresChunkLoad() {
            return false;
        }

        @Override
        public void loadChunk() {
        }
    }

    public static class SetTrackerTask implements ScheduledAction{

        private final TiqualityWorld world;
        private final BlockPos chunkBlockPos;
        private final BlockPos start;
        private final BlockPos end;
        private final TrackerBase tracker;
        private TiqualityChunk chunk = null;

        /**
         * Create a new set tracker task
         * @param world The world
         * @param chunkPos Any location in the chunk you're editing
         * @param start The start position (all chunks)
         * @param end The end position (all chunks)
         * @param tracker The tracker
         */
        public SetTrackerTask(TiqualityWorld world, BlockPos chunkPos, BlockPos start, BlockPos end, TrackerBase tracker) {
            this.world = world;
            this.chunkBlockPos = chunkPos;
            this.start = start;
            this.end = end;
            this.tracker = tracker;
        }

        @Override
        public void run() {
            if(chunk == null){
                throw new IllegalStateException("loadChunk() not called.");
            }
            ChunkPos chunkPos = chunk.getMinecraftChunk().getPos();

            int low_x = Math.max(start.getX(),chunkPos.getXStart());
            int low_y = start.getY();
            int low_z = Math.max(start.getZ(),chunkPos.getZStart());

            int high_x = Math.min(end.getX(), chunkPos.getXEnd());
            int high_y = end.getY();
            int high_z = Math.min(end.getZ(), chunkPos.getZEnd());

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for(int x = low_x; x <= high_x; x++){
                for(int y = low_y; y <= high_y; y++){
                    for(int z = low_z; z <= high_z; z++){
                        pos.setPos(x, y, z);
                        chunk.tiquality_setTrackedPosition(pos, tracker);
                    }
                }
            }
            /*
                Unload chunks when it's done, prevent out of memory errors!
             */
            Chunk mcChunk = chunk.getMinecraftChunk();
            mcChunk.markDirty();
            IChunkProvider provider = mcChunk.getWorld().getChunkProvider();
            if(provider instanceof ChunkProviderServer){
                ((ChunkProviderServer) provider).queueUnload(mcChunk);
            }else if(provider instanceof ChunkProviderClient) {
                ((ChunkProviderClient) provider).unloadChunk(mcChunk.x, mcChunk.z);
            }
        }

        @Override
        public boolean isCallback() {
            return false;
        }

        @Override
        public boolean requiresChunkLoad() {
            return true;
        }

        @Override
        public void loadChunk() {
            chunk = world.getChunk(chunkBlockPos);
        }
    }
}
