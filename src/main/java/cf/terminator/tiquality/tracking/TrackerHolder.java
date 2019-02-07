package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.PersistentData;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cf.terminator.tiquality.tracking.TrackerManager.REGISTERED_TRACKER_TYPES;

/**
 * TrackerHolders are used to manage Trackers so they can be written to disk using unique data.
 * @param <T>
 */
public class TrackerHolder<T extends Tracker> implements Comparable<TrackerHolder>{
    private final T tracker;
    private final long id;

    /**
     * Creates a new TrackerHolder, using a given ID (For loading purposes)
     * @param tracker the tracker
     */
    private TrackerHolder(@Nonnull T tracker, long id) {
        this.tracker = tracker;
        this.id = id;
        tracker.setHolder(this);
        TrackerManager.addTrackerHolder(this);
    }

    /**
     * There's a theoretical maximum of 1.8446744e+19 different Trackers per server. This should suffice.
     */
    public static long generateUniqueTrackerID(){
        PersistentData.NEXT_FREE_TRACKER_ID.lock();
        try {
            if (PersistentData.NEXT_FREE_TRACKER_ID.isSet() == false) {
                PersistentData.NEXT_FREE_TRACKER_ID.setLong(Long.MIN_VALUE);
            }
            long granted = PersistentData.NEXT_FREE_TRACKER_ID.getLong();
            PersistentData.NEXT_FREE_TRACKER_ID.setLong(granted + 1);
            return granted;
        }finally {
            PersistentData.NEXT_FREE_TRACKER_ID.unlock();
        }
    }

    /**
     * Generates a new TrackerHolder, using a new ID (Generation purposes)
     * @param tracker the tracker
     */
    private TrackerHolder(@Nonnull T tracker){
        this.tracker = tracker;
        this.id = generateUniqueTrackerID();
        PersistentData.ID_TO_TRACKER.lock();
        try{
            NBTTagCompound tag = PersistentData.ID_TO_TRACKER.getCompoundTag();
            tag.setTag(Long.toString(id), getHolderTag());
            PersistentData.ID_TO_TRACKER.setCompoundTag(tag);
            tracker.setHolder(this);
        }finally {
            PersistentData.ID_TO_TRACKER.unlock();
        }
        TrackerManager.addTrackerHolder(this);
    }

    /*
        PersistentData.ID_TO_TRACKER

        ROOT
          |
          | ID: TrackerHolder
          | ID: TrackerHolder
          | ID: TrackerHolder

     */

    @Nullable
    public static <T extends Tracker> TrackerHolder<T> getTrackerHolder(TiqualityWorld world, final long id){
        TrackerHolder<T> holder = TrackerManager.foreach(new TrackerManager.Action<TrackerHolder<T>>() {
            @Override
            public void each(Tracker tracker) {
                if(tracker.getHolder().getId() == id){
                    stop(tracker.getHolder());
                }
            }
        });
        if(holder != null){
            return holder;
        }
        NBTTagCompound tag;
        PersistentData.ID_TO_TRACKER.lock();
        try {
            tag = PersistentData.ID_TO_TRACKER.getCompoundTag().getCompoundTag(Long.toString(id));
        }finally {
            PersistentData.ID_TO_TRACKER.unlock();
        }
        if(tag.getSize() == 0){
            return null;
        }
        //noinspection unchecked
        return TrackerHolder.readHolder(world, tag);
    }

    /*
        PersistentData.TRACKER_LOOKUP

        ROOT
          |
          | IDENTIFIER
                    |
                    | HASHCODE
                            |
                            | ID
                            | ID
                            | ID
                            | ID
     */


    /**
     * Creates a new tracker holder, and saves it to disk.
     * @param tracker the tracker
     * @param <T> the tracker
     * @return the holder now containing the tracker.
     */
    public static @Nonnull <T extends Tracker> TrackerHolder<T> createNewTrackerHolder(TiqualityWorld world, @Nonnull T tracker) {
        String trackerIdentifier = tracker.getIdentifier();
        String trackerHashCode = Integer.toString(tracker.getHashCode());

        TrackerHolder<T> holder;
        PersistentData.TRACKER_LOOKUP.lock();
        try{
            NBTTagCompound root = PersistentData.TRACKER_LOOKUP.getCompoundTag();
            NBTTagCompound type = root.getCompoundTag(trackerIdentifier);
            NBTTagList hash = type.getTagList(trackerHashCode, 4);

            if(hash.tagCount() > 0){
                for(NBTBase id : hash){
                    TrackerHolder<T> existingHolder = TrackerHolder.getTrackerHolder(world, ((NBTTagLong) id).getLong());
                    if(existingHolder == null){
                        throw new IllegalStateException("Tried to load a tracker which exists in the system, but it somehow failed: " + id);
                    }
                    if(existingHolder.getTracker().equals(tracker)){
                        return existingHolder;
                    }
                }
            }
            holder = new TrackerHolder<>(tracker);
            hash.appendTag(new NBTTagLong(holder.getId()));
            type.setTag(trackerHashCode, hash);
            root.setTag(trackerIdentifier, type);
            PersistentData.TRACKER_LOOKUP.setCompoundTag(root);
        }finally {
            PersistentData.TRACKER_LOOKUP.unlock();
        }
        try{
            PersistentData.ID_TO_TRACKER.lock();
            NBTTagCompound root = PersistentData.ID_TO_TRACKER.getCompoundTag();
            root.setTag(Long.toString(holder.getId()), holder.getHolderTag());
            PersistentData.ID_TO_TRACKER.setCompoundTag(root);
            return holder;
        }finally{
            PersistentData.ID_TO_TRACKER.unlock();
        }
    }

    /**
     * Reads a trackerholder including it's tracker from file.
     * @param world The world
     * @param tag nbt data
     * @return the tracker, or null if the underlaying tracker failed to load.
     */
    public static @Nullable TrackerHolder readHolder(TiqualityWorld world, NBTTagCompound tag){
        long id = tag.getLong("id");
        Tracker tracker = readTracker(world, tag);
        //noinspection unchecked;
        return tracker == null ? null : new TrackerHolder(tracker, id);
    }

    private static Tracker readTracker(TiqualityWorld world, NBTTagCompound holderTag){
        String type = holderTag.getString("type");
        if(type.equals("")){
            return null;
        }
        Class<? extends Tracker> clazz = REGISTERED_TRACKER_TYPES.get(type);
        if(clazz == null){
            /*
                Either a mod author completely forgot to call cf.terminator.tiquality.api.Tracking.registerCustomTracker(),
                or a mod providing a tracker has been removed since last load.
             */
            return null;
        }
        NBTTagCompound trackerTag = holderTag.getCompoundTag("data");
        try {
            return clazz.newInstance().load(world, trackerTag);
        }catch (Exception e){
            Tiquality.LOGGER.warn("An exception has occurred whilst creating a tracker: " + trackerTag.toString());
            e.printStackTrace();
            return null;
        }
    }

    public NBTTagCompound getHolderTag(){
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", getTracker().getIdentifier());
        tag.setLong("id", getId());
        tag.setTag("data", getTracker().getNBT());
        return tag;
    }

    /**
     * Updates a trackers data by calling getNBT() on the tracker
     * and saves the new value to disk.
     */
    public void update(){
        try{
            PersistentData.ID_TO_TRACKER.lock();
            NBTTagCompound root = PersistentData.ID_TO_TRACKER.getCompoundTag();
            root.setTag(Long.toString(id), getHolderTag());
            PersistentData.ID_TO_TRACKER.setCompoundTag(root);
        }finally{
            PersistentData.ID_TO_TRACKER.unlock();
        }
    }

    @Nonnull
    public T getTracker(){
        return tracker;
    }

    public long getId(){
        return id;
    }

    @Override
    public boolean equals(Object o){
        return o instanceof TrackerHolder && ((TrackerHolder) o).id == this.id;
    }

    @Override
    public int compareTo(@Nonnull TrackerHolder o) {
        return Long.compare(this.id, o.id);
    }
}
