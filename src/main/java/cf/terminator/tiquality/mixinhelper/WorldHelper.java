package cf.terminator.tiquality.mixinhelper;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WorldHelper {

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All lower)
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in it's own seperate thread!
     */
    public static void setTrackerCuboidAsync(TiqualityWorld world, BlockPos start, BlockPos end, TrackerBase tracker, Runnable callback){
        ExecutorService executor = Executors.newCachedThreadPool();

        int low_x = start.getX();
        int low_y = start.getY();
        int low_z = start.getZ();

        int high_x = end.getX();
        int high_y = end.getY();
        int high_z = end.getZ();

        List<TiqualityChunk> chunkList = new ArrayList<>();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for(int x = low_x; x <= high_x + 16; x = x + 16) {
            for (int y = low_y; y <= high_y + 16; y = y + 16) {
                for (int z = low_z; z <= high_z + 16; z = z + 16) {
                    pos.setPos(x,y,z);
                    chunkList.add(world.getChunk(pos));
                }
            }
        }
        for(TiqualityChunk chunk : chunkList){
            executor.execute(new SetTrackerTask(chunk, start, end, tracker));
        }
        executor.shutdown();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

                    //Tiquality.LOGGER.info("Setting ownership of " + ((high_x-low_x) * (high_y-low_y) * (high_z - low_z)) + " blocks took " + (endTime - startTime) + " ms (async)");

                    if(callback != null){
                        callback.run();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static class SetTrackerTask implements Runnable{

        private final TiqualityChunk chunk;
        private final BlockPos start;
        private final BlockPos end;
        private final TrackerBase tracker;

        public SetTrackerTask(TiqualityChunk chunk, BlockPos start, BlockPos end, TrackerBase tracker) {
            this.chunk = chunk;
            this.start = start;
            this.end = end;
            this.tracker = tracker;
        }

        @Override
        public void run() {
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
        }
    }

}
