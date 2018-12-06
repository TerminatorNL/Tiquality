package cf.terminator.tiquality.api;

import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TrackerManager;

public class TrackerAlreadyExistsException extends RuntimeException {

    private final Tracker oldTracker;
    private final Tracker newTracker;

    public TrackerAlreadyExistsException(Tracker oldTracker, Tracker newTracker){
        super(createError(oldTracker, newTracker));
        this.oldTracker = oldTracker;
        this.newTracker = newTracker;

    }

    private static String createError(Tracker oldTracker, Tracker newTracker){

        StringBuilder builder = new StringBuilder();
        TrackerManager.foreach(new TrackerManager.Action<Object>() {
            @Override
            public void each(Tracker tracker) {
                builder.append(tracker.toString());
                builder.append("\r\n");
            }
        });



        return "Tracker already exists.\r\n\r\nExisting tracker: " + oldTracker.toString() +
                "\r\nNew (in error) tracker: " + newTracker.toString() + "\r\n\r\nAll Trackers:\r\n\r\n" + builder.toString();

    }

    public Tracker getOldTracker(){
        return oldTracker;
    }

    public Tracker getNewTracker(){
        return newTracker;
    }
}
