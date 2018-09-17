package cf.terminator.tiquality.store;

import com.mojang.authlib.GameProfile;
import com.sun.istack.internal.NotNull;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class TrackerHub {

    /**
     * Variable holding all PlayerTrackers.
     * All access to it's variables is synchronized.
     */
    private static final TreeMap<UUID, PlayerTracker> TRACKER_LIST = new TreeMap<>();

    /**
     * Gets an unmodifiable set of entries.
     * @return the set
     */
    public static synchronized Set<Map.Entry<UUID, PlayerTracker>> getEntrySet(){
        return Collections.unmodifiableSet(TRACKER_LIST.entrySet());
    }

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     * @param time the time (System.nanoTime()) when the ticking should stop.
     */
    public static synchronized void tickUntil(long time){
        boolean ticked = true;
        Collection<PlayerTracker> trackers = TRACKER_LIST.values();
        while(System.nanoTime() < time && ticked){
            ticked = false;
            for(PlayerTracker tracker : trackers){
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
    public static synchronized void removeInactiveTrackers(){
        ArrayList<UUID> inactiveTrackers = new ArrayList<>();
        for(Map.Entry<UUID, PlayerTracker> entry : TRACKER_LIST.entrySet()){
            PlayerTracker tracker = entry.getValue();
            if(tracker.isDone() && tracker.isLoaded() == false && tracker.isFakeOwner() == false){
                inactiveTrackers.add(entry.getKey());
            }
        }
        for(UUID key : inactiveTrackers){
            TRACKER_LIST.remove(key);
        }
    }

    /**
     * Gets the tracker for a player, if no one exists yet, it will create one. Never returns null.
     * @param profile the profile to bind this tracker to
     * @return the associated PlayerTracker
     */
    public static synchronized @NotNull PlayerTracker getOrCreatePlayerTrackerByProfile(@NotNull final GameProfile profile){
        final PlayerTracker tracker = TRACKER_LIST.get(profile.getId());
        if (tracker == null){
            final PlayerTracker newTracker = new PlayerTracker(profile);
            TRACKER_LIST.put(profile.getId(), newTracker);
            return newTracker;
        }else {
            return tracker;
        }
    }

    /**
     * Gets the tracker for a player, if no one exists yet, it will not create one. Can return null.
     * @param profile the this tracker belongs to.
     * @return the associated PlayerTracker
     */
    public static synchronized @Nullable PlayerTracker getPlayerTrackerByProfile(@NotNull final GameProfile profile){
        return TRACKER_LIST.get(profile.getId());
    }

}