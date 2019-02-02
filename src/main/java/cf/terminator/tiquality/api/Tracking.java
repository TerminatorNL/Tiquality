package cf.terminator.tiquality.api;

import cf.terminator.tiquality.interfaces.Tracker;

import static cf.terminator.tiquality.tracking.TrackerManager.REGISTERED_TRACKER_TYPES;

public class Tracking {

    /**
     * Registers the TrackerBase to the registry, YOU MUST CALL THIS METHOD IN YOUR MOD TO MAKE SURE IT REGISTERS BEFORE THE WORLD IS LOADED!
     * Make sure that your class returns the exact same identifier you provide here.
     */
    public static void registerCustomTracker(String identifier, Class<? extends Tracker> clazz){
        try {
            REGISTERED_TRACKER_TYPES.put(identifier, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
