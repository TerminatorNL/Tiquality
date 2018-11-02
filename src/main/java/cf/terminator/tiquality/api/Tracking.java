package cf.terminator.tiquality.api;

import cf.terminator.tiquality.tracking.TrackerBase;

import static cf.terminator.tiquality.tracking.TrackerBase.REGISTERED_TRACKER_TYPES;

public class Tracking {

    /**
     * Registers the Tracker to the registry, YOU MUST CALL THIS METHOD IN YOUR MOD TO MAKE SURE IT REGISTERS BEFORE THE WORLD IS LOADED!
     * You can use any dummy tracker to initialize, it will only be used to extract the identifier, none of the other methods are called.
     */
    public static void registerCustomTracker(TrackerBase tracker){
        try {
            REGISTERED_TRACKER_TYPES.put(tracker.getIdentifier(), tracker.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
