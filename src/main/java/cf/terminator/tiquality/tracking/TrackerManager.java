package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class TrackerManager {

    /**
     * Variable holding all PlayerTrackers.
     * All access to it's variables is synchronized, for for loops we use getEntrySet()
     */
    private static final Set<TrackerBase> TRACKER_LIST = new HashSet<>();
    static {
        TRACKER_LIST.add(ForcedTracker.INSTANCE);
    }

    /**
     * Loop over the protected set.
     */
    public static <T> T foreach(Action<T> foreach){
        synchronized (TRACKER_LIST){
            for(TrackerBase tracker : TRACKER_LIST){
                foreach.each(tracker);
                if(foreach.stop){
                    return foreach.value;
                }
            }
        }
        return foreach.value;
    }

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     * @param time the time (System.nanoTime()) when the ticking should stop.
     */
    public static void tickUntil(long time){
        while(System.nanoTime() < time && foreach(new Action<Boolean>() {
            @Override
            public void each(TrackerBase tracker) {
                if(tracker.isDone() == false){
                    tracker.grantTick();
                    value = true;
                }
            }
        }) != null){
            /* Woah this is some nasty code formatting. */
        }
    }

    /**
     * Removes trackers which do not tick anymore due to their tickables being unloaded
     */
    public static void removeInactiveTrackers(){
        synchronized (TRACKER_LIST) {
            ArrayList<TrackerBase> inactiveTrackers = new ArrayList<>();
            for (TrackerBase tracker : TRACKER_LIST) {
                if (tracker.isDone() && tracker.isLoaded() == false) {
                    inactiveTrackers.add(tracker);
                } else if (tracker.forceUnload() == true) {
                    inactiveTrackers.add(tracker);
                }
            }
            for (TrackerBase tracker : inactiveTrackers) {
                tracker.onUnload();
                TRACKER_LIST.remove(tracker);
            }
        }
    }

    /**
     * Checks if a Tracker already exists with the same unique ID, if it does: Return the old one and discard the new.
     * If not, return the new one and tracking it.
     * @param input the new Tracker
     * @return the supplied Tracker, or the old one, if it exists.
     */
    public static <T extends TrackerBase> T preventCopies(T input){
        synchronized (TRACKER_LIST) {
            for (TrackerBase tracker : TRACKER_LIST) {
                if (input.getUniqueId() == tracker.getUniqueId()) {
                    //noinspection unchecked
                    return (T) tracker;
                }
            }
            TRACKER_LIST.add(input);
        }
        return input;
    }

    /**
     * Instantiates a new tracker using an NBT compound tag.
     * If the tracker already exists, a reference to the pre-existing tracker is used.
     * @param tagCompound The NBT tag compound
     * @return the tracker
     */
    @Nullable
    public static TrackerBase getTracker(TiqualityWorld world, NBTTagCompound tagCompound){
        String type = tagCompound.getString("type");
        if(type.equals("")){
            return null;
        }
        Class<? extends TrackerBase> clazz = TrackerBase.REGISTERED_TRACKER_TYPES.get(type);
        if(clazz == null){
            /*
                Either a mod author completely forgot to call cf.terminator.tiquality.api.Tracking.registerCustomTracker(),
                or a mod providing a tracker has been removed since last load.
             */
            return null;
        }
        TrackerBase newTracker;
        try {
            newTracker =  clazz.getDeclaredConstructor(TiqualityWorld.class, NBTTagCompound.class).newInstance(world, tagCompound.getCompoundTag("data"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        newTracker.setUniqueId(tagCompound.getLong("id"));
        return preventCopies(newTracker);
    }

    public static abstract class Action<T>{
        public T value = null;
        private boolean stop = false;

        public void stop(T value){
            this.stop = true;
            this.value = value;
        }
        public abstract void each(TrackerBase tracker);
    }
}