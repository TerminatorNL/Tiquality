package cf.terminator.tiquality.api.event;

import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TickLogger;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.fml.common.eventhandler.Event;

@SuppressWarnings("unused")
public class TiqualityEvent extends Event {



    public static class ProfileCompletedEvent extends TiqualityEvent{

        private final TickLogger logger;

        /**
         * The reason we're not sharing a reference to this object is to prevent memory leaks.
         */
        private final PlayerTracker tracker;

        public ProfileCompletedEvent(PlayerTracker tracker, TickLogger logger) {
            this.logger = logger;
            this.tracker = tracker;
        }

        /**
         * Retrieves the collected data
         * @return the TickLogger, which holds all data collected by the profiler.
         */
        public TickLogger getTickLogger(){
            return logger;
        }

        /**
         * Retrieves the owner of the associated PlayerTracker
         * @return The owner.
         */
        public GameProfile getOwner(){
            return tracker.getOwner();
        }

        /**
         * Checks if the owner is a fake owner.
         * Trackers belonging to fake owners are not removed and kept in memory.
         * This method is meant to be overridden.
         *
         * @return true if this is a fake owner.
         */
        public boolean isFakeOwner(){
            return tracker.isFakeOwner();
        }
    }
}
