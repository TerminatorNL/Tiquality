package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.store.PlayerTracker;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TiqualityEntity extends TiqualitySimpleTickable {

    /**
     * Gets the PlayerTracker belonging to this Entity.
     *
     * @return the player tracker
     */
    @Nullable PlayerTracker getPlayerTracker();

    /**
     * Sets the PlayerTracker belonging to this Entity.
     *
     * @param tracker the PlayerTracker
     */
    void setPlayerTracker(@Nullable PlayerTracker tracker);


    /**
     * Gets the persistent entity UUID.
     * @return the entity's UUID.
     */
    UUID getPersistentID();
}
