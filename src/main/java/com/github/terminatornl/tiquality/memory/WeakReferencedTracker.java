package com.github.terminatornl.tiquality.memory;

import com.github.terminatornl.tiquality.interfaces.Tracker;

import java.lang.ref.WeakReference;

public class WeakReferencedTracker extends WeakReference<Tracker> {

    public WeakReferencedTracker(Tracker tracker) {
        super(tracker);
    }

    public boolean exists() {
        Tracker tracker = get();
        return tracker != null;
    }

    /**
     * Used for HashSet
     *
     * @param other o
     * @return e
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof WeakReferencedTracker == false) {
            return false;
        }
        return get() == ((WeakReferencedTracker) other).get();
    }

    /**
     * Used for HashSet
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        Tracker tracker = get();
        return tracker == null ? 0 : tracker.hashCode();
    }
}
