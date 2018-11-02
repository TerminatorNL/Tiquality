package cf.terminator.tiquality.api;

import com.mojang.authlib.GameProfile;
/**
 * Contains exceptions to make sure people who use the API deal with potential problems
 */
public class TiqualityException {
    /**
     * Thrown when a player tracker isn't found in the system.
     */
    public static class PlayerTrackerNotFoundException extends IllegalArgumentException{
        public PlayerTrackerNotFoundException(GameProfile profile){
            super("No PlayerTracker exists for: " + profile.getName() + " with UUID: " + profile.getId());
        }
    }

    /**
     * Thrown when incorrect usage of Tiquality's internals is detected.
     */
    public static class ReadTheDocsException extends RuntimeException{
        public ReadTheDocsException(String text){
            super("Welp! Looks like a programmer didn't read the documentation for Tiquality. Message: " + text);
        }
    }
}
