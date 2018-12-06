package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.PersistentData;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

/**
 * TrackerHolders are used to manage Trackers so they can be written to disk using unique data.
 * This TrackerHolder may be expanded upon in later updates.
 * @param <T>
 */
public class TrackerHolder<T extends Tracker> {
    private static final Object LOCK = new Object();

    private final T tracker;
    private final long id;

    /**
     * Creates a new TrackerHolder, using a given ID (For loading purposes)
     * @param tracker the tracker
     */
    public TrackerHolder(@Nonnull T tracker, long id) {
        this.tracker = tracker;
        this.id = id;
        tracker.setHolder(this);
    }

    /**
     * Generates a new TrackerHolder, using a new ID (Generation purposes)
     * @param tracker the tracker
     */
    private TrackerHolder(@Nonnull T tracker){
        this.tracker = tracker;
        this.id = TrackerManager.generateUniqueTrackerID();
        synchronized (LOCK) {
            String key = tracker.getIdentifier() + tracker.getNBT().toString();
            NBTTagCompound tag = PersistentData.TRACKER_TO_ID.getCompoundTag();
            tag.setLong(key, id);
            PersistentData.TRACKER_TO_ID.setCompoundTag(tag);
        }
        tracker.setHolder(this);
    }

    /**
     * Retrieves a trackerholder, making sure the ID is correct.
     * @param tracker the tracker
     * @param <T> tracker type
     * @return the holder
     */
    public static <T extends Tracker> TrackerHolder<T> getHolder(T tracker){
        String key = tracker.getIdentifier() + tracker.getNBT().toString();
        synchronized (LOCK) {
            if (PersistentData.TRACKER_TO_ID.getCompoundTag().hasKey(key) == false) {
                return new TrackerHolder<>(tracker);
            }
            return new TrackerHolder<>(tracker, PersistentData.TRACKER_TO_ID.getCompoundTag().getLong(key));
        }
    }


    @Nonnull
    public T getTracker(){
        return tracker;
    }

    public long getId(){
        return id;
    }
}
