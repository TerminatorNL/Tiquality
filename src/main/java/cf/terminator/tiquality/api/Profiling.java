package cf.terminator.tiquality.api;

import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TickLogger;
import cf.terminator.tiquality.store.TrackerHub;
import com.mojang.authlib.GameProfile;
/**
 * Contains functions to manage profilers on per-player basis
 */
@SuppressWarnings("unused")
public class Profiling {

    /**
     * Starts the profiler for a player.
     * @param profile The player's gameprofile
     */
    public static void startProfiler(final GameProfile profile) throws TiqualityExceptions.PlayerTrackerNotFoundException {
        PlayerTracker tracker = TrackerHub.getPlayerTrackerByProfile(profile);
        if(tracker == null){
            throw new TiqualityExceptions.PlayerTrackerNotFoundException(profile);
        }
        tracker.setProfileEnabled(true);
    }

    /**
     * Stops the profiler for a player.
     * @param profile The player's gameprofile
     */
    public static void stopProfiler(final GameProfile profile) throws TiqualityExceptions.PlayerTrackerNotFoundException {
        PlayerTracker tracker = TrackerHub.getPlayerTrackerByProfile(profile);
        if(tracker == null){
            throw new TiqualityExceptions.PlayerTrackerNotFoundException(profile);
        }
        tracker.setProfileEnabled(false);
    }

    /**
     * Gets the collected data from a player's PlayerTracker
     * @param profile The player's gameprofile
     * @return the TickLogger associated with the PlayerTracker. Every tracker has one.
     */
    public static TickLogger getTickLogger(final GameProfile profile) throws TiqualityExceptions.PlayerTrackerNotFoundException {
        PlayerTracker tracker = TrackerHub.getPlayerTrackerByProfile(profile);
        if(tracker == null){
            throw new TiqualityExceptions.PlayerTrackerNotFoundException(profile);
        }
        return tracker.getTickLogger();
    }

    /**
     * Resets all collected data inside the player's TickLogger.
     * @param profile The player's gameprofile
     */
    public static void resetTickLogger(final GameProfile profile) throws TiqualityExceptions.PlayerTrackerNotFoundException {
        PlayerTracker tracker = TrackerHub.getPlayerTrackerByProfile(profile);
        if(tracker == null){
            throw new TiqualityExceptions.PlayerTrackerNotFoundException(profile);
        }
        tracker.getTickLogger().reset();
    }
}
