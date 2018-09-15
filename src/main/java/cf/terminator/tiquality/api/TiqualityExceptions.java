package cf.terminator.tiquality.api;

import com.mojang.authlib.GameProfile;
/**
 * Contains exceptions to make sure people who use the API deal with potential problems
 */
public class TiqualityExceptions {
    /**
     * Thrown when a player tracker isn't found in the system.
     */
    public static class PlayerTrackerNotFoundException extends IllegalArgumentException{
        public PlayerTrackerNotFoundException(GameProfile profile){
            super("No PlayerTracker exists for: " + profile.getName() + " with UUID: " + profile.getId());
        }
    }
}
