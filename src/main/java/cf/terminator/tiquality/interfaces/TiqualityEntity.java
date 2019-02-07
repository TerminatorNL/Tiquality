package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TrackerHolder;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TiqualityEntity extends TiqualitySimpleTickable {

    /**
     * Gets the tracker holder belonging to this Entity.
     *
     * @return the tracker holder
     */
    @Nullable
    TrackerHolder getTrackerHolder();

    /**
     * Gets the Tracker belonging to this Entity.
     *
     * @return the tracker
     */
    @Nullable
    Tracker getTracker();

    /**
     * Sets the TrackerHolder belonging to this Entity.
     *
     * @param trackerHolder the PlayerTracker
     */
    void setTrackerHolder(@Nullable TrackerHolder trackerHolder);

    /**
     * Sets the Tracker belonging to this Entity.
     *
     * @param tracker the PlayerTracker
     */
    void setTracker(@Nullable Tracker tracker);

    /**
     * Gets the persistent entity UUID.
     * @return the entity's UUID.
     */
    UUID getPersistentID();
}
