package cf.terminator.tiquality.interfaces.integration.griefprevention;

import cf.terminator.tiquality.tracking.TrackerBase;

public interface TiqualityGPClaim {

    TrackerBase getTiqualityTracker();

    void setTiqualityTracker(TrackerBase tracker);
}
