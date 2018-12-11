package cf.terminator.tiquality.world;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.concurrent.PausableThreadPoolExecutor;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.FiFoQueue;
import cf.terminator.tiquality.util.Utils;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class WorldHelper {

    private static final FiFoQueue<ScheduledAction> TASKS = new FiFoQueue<>();

    /**
     * Sets the tracker in a cuboid area
     * @param corner_1 A corner
     * @param corner_2 The opposite corner
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in the main thread!
     * @param beforeRun a task to run before work starts, This runs in the main thread.
     */
    public static void setTrackerCuboid(TiqualityWorld world, BlockPos corner_1, BlockPos corner_2, Tracker tracker, Runnable callback, Runnable beforeRun){
        BlockPos start = Utils.BlockPos.getMin(corner_1, corner_2);
        BlockPos end = Utils.BlockPos.getMax(corner_1, corner_2);

        int low_x = start.getX();
        int low_z = start.getZ();

        int high_x = end.getX();
        int high_z = end.getZ();

        int affectedChunks = 0;
        synchronized (TASKS) {
            if(beforeRun != null){
                TASKS.addToQueue(new CallBack(beforeRun));
            }
            for (int x = low_x; x <= high_x + 16; x = x + 16) {
                for (int z = low_z; z <= high_z + 16; z = z + 16) {
                    TASKS.addToQueue(new SetTrackerTask(world, new BlockPos(x,0,z), start, end, tracker));
                    affectedChunks++;
                    if(affectedChunks > 40){
                        affectedChunks = 0;
                        TASKS.addToQueue(new SaveWorldTask((World) world));
                    }
                }
            }
            if(callback != null) {
                TASKS.addToQueue(new CallBack(callback));
            }
        }
        if(affectedChunks == 0){
            Tiquality.LOGGER.warn("Tried to set a tracker in an area, but no chunks are affected!");
            Tiquality.LOGGER.warn("Low: " + start);
            Tiquality.LOGGER.warn("High: " + end);
            new Exception().printStackTrace();
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
        private PausableThreadPoolExecutor threadPool = new PausableThreadPoolExecutor(16);

        private SmearedAction() {

        }

        /**
         * This loads all chunks in the main thread,
         * and starts all tasks on per-chunk basis in multiple threads.
         *
         * The main thread will be frozen while these tasks run.
         * When 40 ms have been consumed, it will stop processing more tasks, and
         * waits until all currently running tasks have been processed.
         * After each task exited, the main server thread is continued.
         *
         * This has multiple uses:
         *  * The watchdog will not kill the server, as it still 'ticks'
         *  * Large operations can be submitted at once, and processed later.
         *  * No concurrency errors because the main thread is frozen and therefore does not interact with the chunks
         *  * Because we can assume there are no concurrency errors, we can remove synchronization overhead on per chunk basis, increasing performance.
         *  * Chunks are loaded using the main thread, so Sponge doesn't complain.
         *
         * There are however, downsides to this:
         *  * This process takes a very long time to complete
         *  * While it is processing, the TPS drops if the server was already having performance issues.
         *
         * @param event the event.
         */
        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START){
                return;
            }
            try {
                threadPool.resume();
                long maxTime = System.currentTimeMillis() + 40;
                while (System.currentTimeMillis() < maxTime) {
                    synchronized (TASKS) {
                        if (TASKS.size() == 0) {
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
                            threadPool.pause();
                            action.run();
                            if(TASKS.size() == 0){
                                break;
                            }
                            threadPool.resume();
                        }
                    }
                }
                threadPool.finish();
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

    public static class SaveWorldTask implements ScheduledAction{

        private final World world;

        public SaveWorldTask(World world) {
            this.world = world;
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

        @Override
        public void run() {
            world.getSaveHandler().flush();
        }
    }

    public static class SetTrackerTask implements ScheduledAction{

        private final TiqualityWorld world;
        private final BlockPos chunkBlockPos;
        private final BlockPos start;
        private final BlockPos end;
        private final Tracker tracker;
        private TiqualityChunk chunk = null;

        /**
         * Create a new set tracker task
         * @param world The world
         * @param chunkPos Any location in the chunk you're editing
         * @param start The start position (all chunks)
         * @param end The end position (all chunks)
         * @param tracker The tracker
         */
        public SetTrackerTask(TiqualityWorld world, BlockPos chunkPos, BlockPos start, BlockPos end, Tracker tracker) {
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

            boolean isEntireChunk =
                    chunkPos.getXEnd() == high_x &&
                    chunkPos.getXStart() == low_x &&
                    chunkPos.getZEnd() == high_z &&
                    chunkPos.getZStart() == low_z &&
                    low_y == 0 && high_y == 255;

            if(isEntireChunk) {
                chunk.tiquality_setTrackerForEntireChunk(tracker);
            }else {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

                for (int x = low_x; x <= high_x; x++) {
                    for (int y = low_y; y <= high_y; y++) {
                        for (int z = low_z; z <= high_z; z++) {
                            pos.setPos(x, y, z);
                            chunk.tiquality_setTrackedPosition(pos, tracker);
                        }
                    }
                }
            }
            /*
                Unload chunks when it's done, prevent out of memory errors!
             */
            Chunk mcChunk = chunk.getMinecraftChunk();
            mcChunk.markDirty();
            IChunkProvider provider = mcChunk.getWorld().getChunkProvider();

            if (provider instanceof ChunkProviderServer) {
                ((ChunkProviderServer) provider).queueUnload(mcChunk);
            } else if (provider instanceof ChunkProviderClient) {
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
            if(Tiquality.SPONGE_IS_PRESENT){
                chunk = SpongeChunkLoader.getChunkForced(world, chunkBlockPos);
            }else {
                chunk = world.getTiqualityChunk(chunkBlockPos);
            }
        }
    }
}
