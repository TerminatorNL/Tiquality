package cf.terminator.tiquality.memory;

import cf.terminator.tiquality.interfaces.Tracker;

import java.lang.ref.WeakReference;

public class WeakReferencedTracker extends WeakReference<Tracker> {

    public WeakReferencedTracker(Tracker tracker) {
        super(tracker);
    }

    public boolean isLoaded(){
        Tracker tracker = get();
        return tracker != null && tracker.isLoaded();
    }

    /**
     * Used for HashSet
     * @param other o
     * @return e
     */
    @Override
    public boolean equals(Object other){
        if(other instanceof WeakReferencedTracker == false){
            return false;
        }
        return get() == ((WeakReferencedTracker) other).get();
    }

    /**
     * Used for HashSet
     * @return hashCode
     */
    @Override
    public int hashCode(){
        Tracker tracker = get();
        return tracker == null ? 0 : tracker.hashCode();
    }
}
