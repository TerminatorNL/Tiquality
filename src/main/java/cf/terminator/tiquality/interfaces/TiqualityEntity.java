package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TrackerBase;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TiqualityEntity extends TiqualitySimpleTickable {

    /**
     * Gets the Tracker belonging to this Entity.
     *
     * @return the player tracker
     */
    @Nullable TrackerBase getTracker();

    /**
     * Sets the PlayerTracker belonging to this Entity.
     *
     * @param tracker the PlayerTracker
     */
    void setTracker(@Nullable TrackerBase tracker);


    /**
     * Gets the persistent entity UUID.
     * @return the entity's UUID.
     */
    UUID getPersistentID();
}
