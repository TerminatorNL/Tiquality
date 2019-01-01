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
import net.minecraft.block.state.IBlockState;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Shadow @Final private World world;
    private final BiMap<Byte, Tracker> trackerLookup = HashBiMap.create();
    private final ChunkStorage STORAGE = new ChunkStorage();


    /**
     * Gets the first free index for a player.
     * If Tiquality detects that there are too many owners assigned to this chunk, it wipes all tiquality data in this chunk.
     *
     * All negative numbers are used as markers to mark blocks, and
     * there are 6 reserved values:
     *           0: Does not exists
     *           1: No owner
     *           2: Reserved for potentional future use-case
     *           3: Reserved for potentional future use-case
     *           4: Reserved for potentional future use-case
     *           5: Reserved for potentional future use-case
     *
     *
     * @return the owner value, always lies between 6 and 127 (inclusive)
     */
    private byte getFirstFreeIndex(){
        byte i=6;
        while(trackerLookup.containsKey(i)){
            ++i;
            /* It overflowed, meaning our marker won't work. */
            if(i < 0){
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

    /**
     * Returns the tracker ID
     * @param tracker The tracker
     * @param create If true, it will create an entry for this chunk for that tracker if it does not
     *               exist yet. If it does, we return it's ID.
     *               If false, and the tracker does not exists, we return 1, indicating there's no tracker present
     * @return
     */
    private byte getIDbyTracker(Tracker tracker, boolean create){
        Byte owner_id = trackerLookup.inverse().get(tracker);
        if(owner_id == null){
            if(create == true) {
                owner_id = getFirstFreeIndex();
                trackerLookup.put(owner_id, tracker);
                return owner_id;
            }else{
                return 1;
            }
        }else {
            return owner_id;
        }
    }

    /**
     * Removes unused block owners.
     */
    private void tiquality_refresh(){
        Set<Byte> ownersToKeep = new TreeSet<>();
        for(byte[] data : STORAGE.getAll()){
            for(byte b : data){
                if(b == 1){
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
            STORAGE.set(pos, (byte) 1);
        }else {
            byte id = getIDbyTracker(tracker, true);
            STORAGE.set(pos, id);
            tracker.associateChunk(this);
            trackerLookup.forcePut(id, tracker);
        }
        markDirty();
    }

    /*
     * For sponge, see:
     * cf.terminator.tiquality.mixin.MixinSpongePhaseTracker.onBlockTick
     */
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir){
        Tracker tracker = tiquality_findTrackerByBlockPos(pos);
        if(tracker != null){
            tracker.notifyBlockStateChange((TiqualityWorld) world, pos, state);
        }
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
            byte id = getIDbyTracker(tracker, true);
            STORAGE.setAll(id);
            tracker.associateChunk(this);
            trackerLookup.clear();
            trackerLookup.forcePut(id, tracker);
        }
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
        STORAGE.clearAll();
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

    @Override
    public void replaceTracker(Tracker oldTracker, Tracker newTracker) {
        byte old = getIDbyTracker(oldTracker, false);
        byte new_ = getIDbyTracker(newTracker, false);
        if(old != new_){
            STORAGE.replaceAll(old, new_);
        }
    }

    /**
     * Marks a block position
     * @param pos the pos
     */
    @Override
    public void tiquality_mark(BlockPos pos){
        STORAGE.mark(pos);
    }

    /**
     * Unmarks a block position
     * @param pos the pos
     */
    @Override
    public void tiquality_unMark(BlockPos pos){
        STORAGE.unMark(pos);
    }

    /**
     * Checks if a position is marked.
     * @param pos the position
     * @return marked
     */
    @Override
    public boolean tiquality_isMarked(BlockPos pos){
        return STORAGE.isMarked(pos);
    }
}
