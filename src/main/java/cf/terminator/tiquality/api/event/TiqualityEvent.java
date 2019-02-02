package cf.terminator.tiquality.api.event;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class TiqualityEvent extends Event {


    public static class ProfileCompletedEvent extends TiqualityEvent{

        private final TickLogger logger;
        private final Tracker tracker;

        public ProfileCompletedEvent(Tracker tracker, TickLogger logger) {
            this.logger = logger.copy();
            this.tracker = tracker;
        }

        /**
         * Gets the associated tracker
         *
         * To prevent memory leaks, DO NOT KEEP THIS VARIABLE IN MEMORY!
         */
        public Tracker getTracker(){
            return tracker;
        }

        /**
         * Retrieves the collected data
         * @return the TickLogger, which holds all data collected by the profiler.
         */
        public TickLogger getTickLogger(){
            return logger;
        }

    }

    /**
     * Fired when a tracker is assigned to a blockpos.
     * Canceling this event will cause no tracker to be assigned.
     *
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetBlockTrackerEvent extends TiqualityEvent{

        private Tracker tracker;
        private final TiqualityChunk chunk;
        private final BlockPos pos;

        public SetBlockTrackerEvent(TiqualityChunk chunk, BlockPos pos, Tracker tracker){
            this.tracker = tracker;
            this.chunk = chunk;
            this.pos = pos;
        }

        public TiqualityChunk getChunk(){
            return chunk;
        }

        public TiqualityWorld getTiqualityWorld(){
            return (TiqualityWorld) chunk.getMinecraftChunk().getWorld();
        }

        public World getMinecraftWorld(){
            return chunk.getMinecraftChunk().getWorld();
        }

        public BlockPos getPos(){
            return pos;
        }

        public Tracker getTracker(){
            return tracker;
        }

        public void setTracker(Tracker tracker){
            this.tracker = tracker;
        }

    }

    /**
     * Fired when a tracker is assigned to an entire chunk at once.
     * Canceling this event will cause no tracker to be assigned.
     *
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetChunkTrackerEvent extends TiqualityEvent{

        private Tracker tracker;
        private final TiqualityChunk chunk;
        private boolean perBlockMode = false;

        public SetChunkTrackerEvent(TiqualityChunk chunk, Tracker tracker){
            this.tracker = tracker;
            this.chunk = chunk;
        }

        public TiqualityChunk getChunk(){
            return chunk;
        }

        public TiqualityWorld getTiqualityWorld(){
            return (TiqualityWorld) chunk.getMinecraftChunk().getWorld();
        }

        public World getMinecraftWorld(){
            return chunk.getMinecraftChunk().getWorld();
        }

        public Tracker getTracker(){
            return tracker;
        }

        public void setTracker(Tracker tracker){
            this.tracker = tracker;
        }

        public void setPerBlockMode(){
            perBlockMode = true;
        }

        public boolean isPerBlockMode(){
            return perBlockMode;
        }

    }

    /**
     * Fired when a tracker is assigned to an entity.
     * Canceling this event will cause no tracker to be assigned.
     *
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetEntityTrackerEvent extends TiqualityEvent{

        private Tracker tracker;
        private final TiqualityEntity entity;

        public SetEntityTrackerEvent(TiqualityEntity entity, @Nullable Tracker tracker){
            this.tracker = tracker;
            this.entity = entity;
        }

        public TiqualityEntity getEntity(){
            return entity;
        }

        public TiqualityWorld getTiqualityWorld(){
            return (TiqualityWorld) entity.getWorld();
        }

        public World getMinecraftWorld(){
            return entity.getWorld();
        }

        public Tracker getTracker(){
            return tracker;
        }

        public void setTracker(Tracker tracker){
            this.tracker = tracker;
        }

    }
}
