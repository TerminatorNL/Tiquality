package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("WeakerAccess")
public class TrackerManager {

    /**
     * Holds a list of all registered trackers
     * See: cf.terminator.tiquality.api.Tracking#registerCustomTracker(java.lang.Class)
     */
    public static final HashMap<String, Class<? extends Tracker>> REGISTERED_TRACKER_TYPES = new HashMap<>();

    /**
     * Variable holding all TrackerHolders, and thus: trackers.
     */
    private static final TreeMap<Long, TrackerHolder> TRACKER_LIST = new TreeMap<>();
    private static final ReentrantLock TRACKER_LIST_LOCK = new ReentrantLock();
    /**
     * Loop over the protected set.
     */
    public static <T> T foreach(Action<T> foreach){
        TRACKER_LIST_LOCK.lock();
        try {
            for (TrackerHolder holder : TRACKER_LIST.values()) {
                foreach.each(holder.getTracker());
                if (foreach.stop) {
                    break;
                }
            }
            return foreach.value;
        } catch (ConcurrentModificationException e) {
            Tiquality.LOGGER.warn("Concurrent modification exception occurred! To prevent a crash, we caught it. I need more eyes for this. See issues: #20 #22 and #41");
            e.printStackTrace();
            return foreach.value;
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    /**
     * Gets a tracker by ID, but ONLY if it's currently loaded.
     */
    @Nullable
    public static Tracker getTrackerByID(long id){
        TRACKER_LIST_LOCK.lock();
        try{
            TrackerHolder holder = TRACKER_LIST.get(id);
            return holder == null ? null : holder.getTracker();
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     * @param time the time (System.nanoTime()) when the ticking should stop.
     */
    public static void tickUntil(long time){
        TRACKER_LIST_LOCK.lock();
        try {
            boolean hasWork = true;

            // Shallow copy to prevent ConcurrentModificationExceptions.
            LinkedList<TrackerHolder> copiedHolders = new LinkedList<>(TRACKER_LIST.values());

            while (System.nanoTime() < time && hasWork) {
                hasWork = false;
                for (TrackerHolder holder : copiedHolders) {
                    Tracker tracker = holder.getTracker();
                    if (tracker.needsTick()) {
                        hasWork = true;
                        tracker.grantTick();
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            Tiquality.LOGGER.warn("Concurrent modification exception occurred! To prevent a crash, we caught it. I need more eyes for this. See issues: #20 #22 and #41");
            e.printStackTrace();
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    /**
     * Removes trackers which do not tick anymore due to their tickables being unloaded
     */
    public static void removeInactiveTrackers(){
        TRACKER_LIST_LOCK.lock();
        try {
            TRACKER_LIST.entrySet().removeIf(entry -> {
                Tracker tracker = entry.getValue().getTracker();
                if (tracker.shouldUnload()) {
                    tracker.onUnload();
                    return true;
                } else {
                    return false;
                }
            });
        } catch (ConcurrentModificationException e) {
            Tiquality.LOGGER.warn("Concurrent modification exception occurred! To prevent a crash, we caught it. I need more eyes for this. See issues: #20 #22 and #41");
            e.printStackTrace();
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    /**
     * Instantiates a new tracker using an NBT compound tag.
     * If the tracker already exists, a reference to the pre-existing tracker is given.
     * @param tagCompound The NBT tag compound
     * @return the tracker
     */
    @Nullable
    public static TrackerHolder readHolder(TiqualityWorld world, NBTTagCompound tagCompound){
        String type = tagCompound.getString("type");
        if(type.equals("")){
            return null;
        }
        long id = tagCompound.getLong("id");
        TRACKER_LIST_LOCK.lock();
        try {
            TrackerHolder holder = TRACKER_LIST.get(id);
            if (holder != null) {
                return holder;
            }
            holder = TrackerHolder.readHolder(world, tagCompound);
            if (holder == null) {
                return null;
            }
            return holder;
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    /**
     * Creates a new tracker, and saves it to disk.
     * @param tracker The tracker, it must be a newly created tracker!
     * @param <T> The tracker.
     * @throws IllegalStateException if the tracker already has a holder assigned, indicative of a programming error.
     * @return the tracker holder
     */
    @Nonnull
    public static <T extends Tracker> TrackerHolder<T> createNewTrackerHolder(TiqualityWorld world, T tracker){
        TRACKER_LIST_LOCK.lock();
        try{
            TrackerHolder<T> holder = tracker.getHolder();
            if(holder != null){
                throw new IllegalStateException("This tracker wants to be saved as if it was new, but it's not! : " + tracker.toString());
            }
            return TrackerHolder.createNewTrackerHolder(world, tracker);
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    public static void addTrackerHolder(TrackerHolder holder){
        TRACKER_LIST_LOCK.lock();
        try{
            if(TRACKER_LIST.put(holder.getId(), holder) != null){
                throw new IllegalStateException("Attempted to save two different trackerholder instances!");
            }
        }finally {
            TRACKER_LIST_LOCK.unlock();
        }
    }

    public static abstract class Action<T>{
        public T value = null;
        private boolean stop = false;

        public void stop(T value){
            this.stop = true;
            this.value = value;
        }
        public abstract void each(Tracker tracker);
    }
}