package cf.terminator.tiquality.api;

import cf.terminator.tiquality.interfaces.Tracker;

import static cf.terminator.tiquality.tracking.TrackerManager.REGISTERED_TRACKER_TYPES;

public class Tracking {

    /**
     * Registers the TrackerBase to the registry, YOU MUST CALL THIS METHOD IN YOUR MOD TO MAKE SURE IT REGISTERS BEFORE THE WORLD IS LOADED!
     * You can use any dummy tracker to initialize, it will only be used to extract the identifier, none of the other methods are called.
     */
    public static void registerCustomTracker(String identifier, Class<? extends Tracker> clazz){
        try {
            REGISTERED_TRACKER_TYPES.put(identifier, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
