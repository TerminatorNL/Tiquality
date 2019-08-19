package com.github.terminatornl.tiquality.api.event;

import com.github.terminatornl.tiquality.interfaces.TiqualityChunk;
import com.github.terminatornl.tiquality.interfaces.TiqualityEntity;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.profiling.TickLogger;
import com.github.terminatornl.tiquality.tracking.TrackerHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class TiqualityEvent extends Event {


    public static class ProfileCompletedEvent extends TiqualityEvent {

        private final TickLogger logger;
        private final Tracker tracker;

        /**
         * Sent after a profiler has finished. Do not change the TickLogger here. Read only.
         *
         * @param tracker t
         * @param logger  l
         */
        public ProfileCompletedEvent(Tracker tracker, TickLogger logger) {
            this.logger = logger;
            this.tracker = tracker;
        }

        /**
         * Gets the associated holder
         * <p>
         * To prevent memory leaks, DO NOT KEEP THIS VARIABLE IN MEMORY!
         */
        public Tracker getTracker() {
            return tracker;
        }

        /**
         * Retrieves the collected data
         *
         * @return the TickLogger, which holds all data collected by the profiler.
         */
        public TickLogger getTickLogger() {
            return logger;
        }

    }

    /**
     * Fired when a holder is assigned to a blockpos.
     * Canceling this event will cause no holder to be assigned.
     * <p>
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetBlockTrackerEvent extends TiqualityEvent {

        private final TiqualityChunk chunk;
        private final BlockPos pos;
        private Tracker tracker;

        public SetBlockTrackerEvent(TiqualityChunk chunk, BlockPos pos, Tracker tracker) {
            this.tracker = tracker;
            this.chunk = chunk;
            this.pos = pos;
        }

        public TiqualityChunk getChunk() {
            return chunk;
        }

        public TiqualityWorld getTiqualityWorld() {
            return (TiqualityWorld) chunk.getMinecraftChunk().getWorld();
        }

        public World getMinecraftWorld() {
            return chunk.getMinecraftChunk().getWorld();
        }

        public BlockPos getPos() {
            return pos;
        }

        public Tracker getTracker() {
            return tracker;
        }

        public void setTracker(Tracker tracker) {
            this.tracker = tracker;
        }

    }

    /**
     * Fired when a holder is assigned to an entire chunk at once.
     * Canceling this event will cause no holder to be assigned.
     * <p>
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetChunkTrackerEvent extends TiqualityEvent {

        private final TiqualityChunk chunk;
        private Tracker tracker;
        private boolean perBlockMode = false;

        public SetChunkTrackerEvent(TiqualityChunk chunk, Tracker tracker) {
            this.tracker = tracker;
            this.chunk = chunk;
        }

        public TiqualityChunk getChunk() {
            return chunk;
        }

        public TiqualityWorld getTiqualityWorld() {
            return (TiqualityWorld) chunk.getMinecraftChunk().getWorld();
        }

        public World getMinecraftWorld() {
            return chunk.getMinecraftChunk().getWorld();
        }

        public Tracker getTracker() {
            return tracker;
        }

        public void setTracker(Tracker tracker) {
            this.tracker = tracker;
        }

        public void setPerBlockMode() {
            perBlockMode = true;
        }

        public boolean isPerBlockMode() {
            return perBlockMode;
        }

    }

    /**
     * Fired when a holder is assigned to an entity.
     * Canceling this event will cause no holder to be assigned.
     * <p>
     * Remember: This can be called ASYNC. if you need to access the world, make sure it runs in the main thread.
     */
    @Cancelable
    public static class SetEntityTrackerEvent extends TiqualityEvent {

        private final TiqualityEntity entity;
        private TrackerHolder holder;

        public SetEntityTrackerEvent(TiqualityEntity entity, @Nullable TrackerHolder holder) {
            this.holder = holder;
            this.entity = entity;
        }

        public TiqualityEntity getEntity() {
            return entity;
        }

        public TiqualityWorld getTiqualityWorld() {
            return (TiqualityWorld) entity.tiquality_getWorld();
        }

        public World getMinecraftWorld() {
            return entity.tiquality_getWorld();
        }

        @Nullable
        public TrackerHolder getHolder() {
            return holder;
        }

        public void setHolder(@Nullable TrackerHolder holder) {
            this.holder = holder;
        }

    }
}
