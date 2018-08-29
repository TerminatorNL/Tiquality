package cf.terminator.tiquality.store;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import com.sun.istack.internal.NotNull;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class TrackerHub {

    /**
     * Variable holding all PlayerTrackers. It's important to note that you
     * MUST NEVER USE THIS IN ANY OTHER THREAD THAN THE MAIN SERVER THREAD.
     *
     * If you are not entirely sure if you're in the main thread, use TiqualityCommand.SCHEDULER#run()
     */
    public static final TreeMap<UUID, PlayerTracker> UNSAFE_SERVER_THREAD_ONLY_TRACKER = new TreeMap<>();

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     */
    public static void tickUntil(long time){
        boolean ticked = true;
        Collection<PlayerTracker> trackers = UNSAFE_SERVER_THREAD_ONLY_TRACKER.values();
        while(System.nanoTime() < time && ticked){
            ticked = false;
            for(PlayerTracker tracker : trackers){
                if(tracker.isDone()){
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
        ArrayList<UUID> inactiveTrackers = new ArrayList<>();
        for(Map.Entry<UUID, PlayerTracker> entry : UNSAFE_SERVER_THREAD_ONLY_TRACKER.entrySet()){
            PlayerTracker tracker = entry.getValue();
            if(tracker.isDone() == true && tracker.isFakeOwner() == false){
                inactiveTrackers.add(entry.getKey());
            }
        }
        for(UUID key : inactiveTrackers){
            UNSAFE_SERVER_THREAD_ONLY_TRACKER.remove(key);
        }
    }

    /**
     * Gets the tracker for a player, if no one exists yet, it will create one. Never returns null.
     * @param profile the profile to bind this tracker to
     * @return the associated PlayerTracker
     */
    public static @NotNull PlayerTracker getPlayerTrackerSafeByProfile(@NotNull final GameProfile profile){
        final PlayerTracker tracker = SynchronizedAction.run(new SynchronizedAction.Action<PlayerTracker>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<PlayerTracker> variable) {
                variable.set(UNSAFE_SERVER_THREAD_ONLY_TRACKER.get(profile.getId()));
            }
        });
        if (tracker == null){
            final PlayerTracker newTracker = new PlayerTracker(profile);
            Tiquality.SCHEDULER.scheduleWait(new Runnable() {
                @Override
                public void run() {
                    UNSAFE_SERVER_THREAD_ONLY_TRACKER.put(profile.getId(), newTracker);
                }
            });
            return newTracker;
        }else {
            return tracker;
        }
    }

}