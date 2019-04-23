package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TrackerHolder;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

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

    ResourceLocation tiquality_getResourceLocation();
}
