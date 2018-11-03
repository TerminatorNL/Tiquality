package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import com.mojang.authlib.GameProfile;
import com.sun.istack.internal.NotNull;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class TrackerManager {

    /**
     * Variable holding all PlayerTrackers.
     * All access to it's variables is synchronized.
     */
    private static final Set<TrackerBase> TRACKER_LIST = Collections.synchronizedSet(new HashSet<>());
    static {
        TRACKER_LIST.add(ForcedTracker.INSTANCE);
    }

    /**
     * Gets an unmodifiable set of entries.
     * @return the set
     */
    public static Set<TrackerBase> getEntrySet(){
        return new HashSet<>(TRACKER_LIST);
    }

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     * @param time the time (System.nanoTime()) when the ticking should stop.
     */
    public static void tickUntil(long time){
        boolean ticked = true;
        while(System.nanoTime() < time && ticked){
            ticked = false;
            for(TrackerBase tracker : TRACKER_LIST){
                if(tracker.isDone() == false){
                    tracker.grantTick();
                    ticked = true;
                }
            }
        }
    }

    /**
     * Removes trackers which do not tick anymore due to their tickables being unloaded
     */
    public static void removeInactiveTrackers(){
        ArrayList<TrackerBase> inactiveTrackers = new ArrayList<>();
        for(TrackerBase tracker : TRACKER_LIST){
            if(tracker.isDone() && tracker.isLoaded() == false){
                inactiveTrackers.add(tracker);
            }else if(tracker.forceUnload() == true){
                inactiveTrackers.add(tracker);
            }
        }
        for(TrackerBase tracker : inactiveTrackers){
            tracker.onUnload();
            TRACKER_LIST.remove(tracker);
        }
    }

    /**
     * Checks if a Tracker already exists with the same unique ID, if it does: Return the old one and discard the new.
     * If not, return the new one and tracking it.
     * @param input the new Tracker
     * @return the supplied Tracker, or the old one, if it exists.
     */
    public static <T extends TrackerBase> T preventCopies(T input){
        for(TrackerBase tracker : TRACKER_LIST){
            if(input.getUniqueId() == tracker.getUniqueId()){
                //noinspection unchecked
                return (T) tracker;
            }
        }
        TRACKER_LIST.add(input);
        return input;
    }

    /*
     * Gets the tracker for a player, if no one exists yet, it will create one. Never returns null.
     * @param profile the profile to bind this tracker to the profile MUST contain an UUID!
     * @return the associated PlayerTracker
     */
    public static @NotNull PlayerTracker getOrCreatePlayerTrackerByProfile(@NotNull final GameProfile profile){
        UUID id = profile.getId();
        if(id == null){
            throw new IllegalArgumentException("GameProfile must have an UUID");
        }
        for(TrackerBase tracker : TRACKER_LIST){
            if(tracker instanceof PlayerTracker){
                PlayerTracker playerTracker = (PlayerTracker) tracker;
                if(playerTracker.getOwner().equals(profile)){
                    return playerTracker;
                }
            }
        }
        final PlayerTracker newTracker = new PlayerTracker(profile);
        TRACKER_LIST.add(newTracker);
        return newTracker;
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
}