package cf.terminator.tiquality.interfaces;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TiqualityEntity extends TiqualitySimpleTickable {

    /**
     * Gets the TrackerBase belonging to this Entity.
     *
     * @return the player tracker
     */
    @Nullable
    Tracker getTracker();

    /**
     * Sets the PlayerTracker belonging to this Entity.
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
