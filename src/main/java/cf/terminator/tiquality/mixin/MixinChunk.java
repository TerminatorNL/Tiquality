package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TrackerHolder;
import cf.terminator.tiquality.tracking.TrackerManager;
import cf.terminator.tiquality.world.ChunkStorage;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class MixinChunk implements TiqualityChunk {

    @Shadow public abstract boolean isLoaded();

    @Shadow public abstract World getWorld();

    @Shadow @Final public int x;
    @Shadow @Final public int z;

    @Shadow public abstract void markDirty();

    private final BiMap<Byte, Tracker> trackerLookup = HashBiMap.create();
    private final ChunkStorage STORAGE = new ChunkStorage();

    /**
     * Gets the first free index for a player.
     * If Tiquality detects that there are too many owners assigned to this chunk, it wipes all data in this chunk.
     *
     * There are 6 reserved values:
     *           0: No owner
     *          -1: Reserved for potentional future use-case
     *          -2: Reserved for potentional future use-case
     *          -3: Reserved for potentional future use-case
     *          -4: Reserved for potentional future use-case
     *          -5: Reserved for potentional future use-case
     * @return the owner value
     */
    private byte getFirstFreeIndex(){
        byte i=1;
        while(trackerLookup.containsKey(i)){
            ++i;
            if(i == -5){
                trackerLookup.clear();
                STORAGE.clearAll();
                Tiquality.LOGGER.warn("There are too many owners in this chunk: " + this.getWorld().provider.getDimension() + " X=" + this.x + " Z=" + this.z);
                Tiquality.LOGGER.warn("All tracking elements in this chunk have been removed to prevent undefined behavior.");

                /* It's now safe to assume that recursion cannot occur. */
                return getFirstFreeIndex();
            }
        }
        return i;
    }

    private byte getIDbyTracker(Tracker tracker){
        Byte owner_id = trackerLookup.inverse().get(tracker);
        if(owner_id == null){
            owner_id = getFirstFreeIndex();
            trackerLookup.put(owner_id, tracker);
        }
        return owner_id;
    }

    /**
     * Removes unused block owners.
     */
    private void tiquality_refresh(){
        Set<Byte> ownersToKeep = new TreeSet<>();
        for(byte[] data : STORAGE.getAll()){
            for(byte b : data){
                if(b == 0){
                    continue;
                }
                if(ownersToKeep.contains(b) == false){
                    ownersToKeep.add(b);
                }
            }
        }

        trackerLookup.keySet().retainAll(ownersToKeep);
    }

    @Override
    public Chunk getMinecraftChunk(){
        return (Chunk) (Object) this;
    }

    @Override
    public void tiquality_setTrackedPosition(BlockPos pos, Tracker tracker){

        TiqualityEvent.SetBlockTrackerEvent event = new TiqualityEvent.SetBlockTrackerEvent(this, pos, tracker);

        if(MinecraftForge.EVENT_BUS.post(event) /* if cancelled */){
            return;
        }
        tracker = event.getTracker();

        if(tracker == null){
            STORAGE.set(pos, (byte) 0);
        }else {
            byte id = getIDbyTracker(tracker);
            STORAGE.set(pos, id);
            tracker.associateChunk(this);
            trackerLookup.forcePut(id, tracker);
        }
        markDirty();
    }

    @Override
    public void tiquality_setTrackerForEntireChunk(Tracker tracker){

        TiqualityEvent.SetChunkTrackerEvent event = new TiqualityEvent.SetChunkTrackerEvent(this, tracker);

        if(MinecraftForge.EVENT_BUS.post(event) /* if cancelled */){
            return;
        }
        tracker = event.getTracker();

        if(tracker == null){
            STORAGE.clearAll();
        }else {
            byte id = getIDbyTracker(tracker);
            STORAGE.setAll(id);
            tracker.associateChunk(this);
            trackerLookup.forcePut(id, tracker);
        }
        tiquality_refresh();
        markDirty();
    }

    @Override
    public void tiquality_writeToNBT(NBTTagCompound tag) {
        tiquality_refresh();
        NBTTagList list = tag.getTagList("Sections", 10);
        STORAGE.injectNBTAfter(list);
        tag.setTag("Sections", list);
        NBTTagList trackerList = new NBTTagList();
        for(Map.Entry<Byte, Tracker> e : trackerLookup.entrySet()){
            if(e.getValue().shouldSaveToDisk() == false){
                continue;
            }
            NBTTagCompound trackerData = new NBTTagCompound();
            trackerData.setByte("chunk_id", e.getKey());
            trackerData.setTag("tracker", TrackerManager.getTrackerTag(e.getValue().getHolder()));
            trackerList.appendTag(trackerData);
        }
        if(trackerList.tagCount() > 0) {
            tag.setTag("Tiquality", trackerList);
        }
    }

    @Override
    public void tiquality_loadNBT(World world, NBTTagCompound tag) {
        STORAGE.loadFromNBT(tag.getTagList("Sections", 10));

        for (NBTBase nbtBase : tag.getTagList("Tiquality", 10)) {
            NBTTagCompound trackerData = (NBTTagCompound) nbtBase;
            TrackerHolder holder = TrackerManager.getTracker((TiqualityWorld) world, trackerData.getCompoundTag("tracker"));
            if(holder != null){
                trackerLookup.forcePut(trackerData.getByte("chunk_id"), holder.getTracker());
            }else{
                Tiquality.LOGGER.debug("Failed to load tracker in chunk: ", this);
            }
        }
    }

    @Override
    public @Nullable
    Tracker tiquality_findTrackerByBlockPos(BlockPos pos){
        return trackerLookup.get(STORAGE.get(pos));
    }

    @Override
    public boolean isChunkLoaded(){
        return isLoaded();
    }

    @Override
    public int compareTo(@Nonnull TiqualityChunk other){
        ChunkPos thisPos = ((Chunk) (Object) this).getPos();
        ChunkPos otherPos = ((Chunk) other).getPos();

        int xComp = Integer.compare(thisPos.x, otherPos.x);
        return xComp != 0 ? xComp : Integer.compare(thisPos.z, otherPos.z);
    }

    @Override
    public void associateTrackers() {
        for (Tracker tracker : trackerLookup.values()) {
            tracker.associateChunk(this);
        }
    }

}
