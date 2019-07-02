package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TrackerHolder;
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
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Mixin(value = Chunk.class, priority = 2000)
public abstract class MixinChunk implements TiqualityChunk {

    @Shadow public abstract boolean isLoaded();

    @Shadow public abstract World getWorld();

    @Shadow @Final public int x;
    @Shadow @Final public int z;

    @Shadow public abstract void markDirty();

    @Shadow @Final private World world;

    @Shadow public abstract ChunkPos getPos();

    private final BiMap<Byte, Tracker> tiquality_trackerLookup = HashBiMap.create();
    private ChunkStorage tiquality_STORAGE;
    private int tiquality_modcount = 0;
    private int tiquality_refreshmod = 0;

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void tiquality_onInitialization_short(World worldIn, int x, int z, CallbackInfo ci){
        tiquality_STORAGE = new ChunkStorage(this);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V", at = @At("RETURN"))
    private void tiquality_onInitialization_extended(World worldIn, ChunkPrimer primer, int x, int z, CallbackInfo ci){
        tiquality_STORAGE = new ChunkStorage(this);
    }

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
        while(tiquality_trackerLookup.containsKey(i)){
            ++i;
            /* It overflowed, meaning our marker won't work. */
            if(i < 0){
                tiquality_trackerLookup.clear();
                tiquality_STORAGE.clearAll();
                tiquality_modcount++;
                tiquality_refresh();
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
     * @return tracker ID
     */
    private byte getIDbyTracker(Tracker tracker, boolean create){
        Byte owner_id = tiquality_trackerLookup.inverse().get(tracker);
        if(owner_id == null){
            if(create == true) {
                owner_id = getFirstFreeIndex();
                tiquality_trackerLookup.put(owner_id, tracker);
                tiquality_modcount++;
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
        if(tiquality_refreshmod == tiquality_modcount){
            return;
        }

        Set<Byte> ownersToKeep = new TreeSet<>();
        for(byte[] data : tiquality_STORAGE.getAll()){
            for(byte b : data){
                if(b == 1 || b == 0){
                    continue;
                }
                if(ownersToKeep.contains(b) == false){
                    ownersToKeep.add(b);
                }
            }
        }

        tiquality_trackerLookup.keySet().retainAll(ownersToKeep);
        tiquality_refreshmod = tiquality_modcount;
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
            tiquality_STORAGE.set(pos, (byte) 0);
        }else {
            byte id = getIDbyTracker(tracker, true);
            tiquality_STORAGE.set(pos, id);
            tracker.associateChunk(this);
            tiquality_trackerLookup.forcePut(id, tracker);
        }

        tiquality_modcount++;
        markDirty();
    }

    /*
     * For sponge, see:
     * cf.terminator.tiquality.mixin.MixinWorldServerSponge.onSetBlockState
     */
    @Inject(method = "setBlockState", at = @At("HEAD"), require = 1)
    private void onSetBlockState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir){
        onSetBlockStateHook(pos, state);
    }

    @Override
    public void onSetBlockStateHook(BlockPos pos, IBlockState state){
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
        if(event.isPerBlockMode()){
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int low_x = getPos().getXStart();
            int low_z = getPos().getZStart();
            int high_x = getPos().getXEnd();
            int high_z = getPos().getZEnd();

            for (int x = low_x; x <= high_x; x++) {
                for (int y = 0; y <= 255; y++) {
                    for (int z = low_z; z <= high_z; z++) {
                        pos.setPos(x, y, z);
                        tiquality_setTrackedPosition(pos, tracker);
                    }
                }
            }
            return;
        }

        tracker = event.getTracker();

        if(tracker == null){
            tiquality_STORAGE.clearAll();
        }else {
            byte id = getIDbyTracker(tracker, true);
            tiquality_STORAGE.setAll(id);
            tracker.associateChunk(this);
            tiquality_trackerLookup.clear();
            tiquality_trackerLookup.forcePut(id, tracker);
        }
        tiquality_modcount++;
        markDirty();
    }

    @Override
    @Nullable
    public NBTTagCompound tiquality_getNBT() {
        tiquality_refresh();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound storageTag = tiquality_STORAGE.getNBT();
        if(storageTag != null) {
            tag.setTag("Storage", storageTag);
        }
        NBTTagList trackerList = new NBTTagList();
        for (Map.Entry<Byte, Tracker> e : tiquality_trackerLookup.entrySet()) {
            if (e.getValue().shouldSaveToDisk() == false) {
                continue;
            }
            NBTTagCompound trackerData = new NBTTagCompound();
            trackerData.setByte("chunk_id", e.getKey());
            trackerData.setLong("tracker", e.getValue().getHolder().getId());
            trackerList.appendTag(trackerData);
        }
        if (trackerList.tagCount() > 0) {
            tag.setTag("Trackers", trackerList);
        }
        return tag.getSize() == 0 ? null : tag;
    }

    @Override
    public void tiquality_loadNBT(World world, NBTTagCompound tag) {
        tiquality_STORAGE.clearAll();
        tiquality_STORAGE.loadFromNBT(tag.getCompoundTag("Storage"), getMinecraftChunk());
        for (NBTBase nbtBase : tag.getTagList("Trackers", 10)) {
            NBTTagCompound trackerData = (NBTTagCompound) nbtBase;
            long id = trackerData.getLong("tracker");
            if(id == 0){
                Tiquality.LOGGER.debug("Failed to load tracker in chunk: ", this);
                continue;
            }
            TrackerHolder holder = TrackerHolder.getTrackerHolder((TiqualityWorld) world, id);
            if(holder != null){
                holder.getTracker().associateChunk(this);
                tiquality_trackerLookup.forcePut(trackerData.getByte("chunk_id"), holder.getTracker());
            }else{
                Tiquality.LOGGER.debug("Failed to load tracker with ID " + id + " in chunk: " + this);
            }
        }
        tiquality_STORAGE.recalculateDominatingTracker();
    }

    @Override
    public @Nullable
    Tracker tiquality_findTrackerByBlockPos(BlockPos pos){
        return tiquality_trackerLookup.get(tiquality_STORAGE.get(pos));
    }

    @Override
    public Set<Tracker> getActiveTrackers(){
        tiquality_refresh();
        return Collections.unmodifiableSet(tiquality_trackerLookup.values());
    }

    @Override
    @Nullable
    public Tracker getCachedMostDominantTracker(){
        return tiquality_trackerLookup.get(tiquality_STORAGE.getDominatingTracker());
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
        for (Tracker tracker : tiquality_trackerLookup.values()) {
            tracker.associateChunk(this);
        }
    }

    @Override
    public void replaceTracker(Tracker oldTracker, Tracker newTracker) {
        byte old = getIDbyTracker(oldTracker, false);
        byte new_ = getIDbyTracker(newTracker, false);
        if(old != new_){
            if(new_ == 1){
                tiquality_STORAGE.replaceAll(old, (byte) 0);
            }else{
                tiquality_STORAGE.replaceAll(old, new_);
            }
        }
        tiquality_modcount++;
        markDirty();
    }

    /**
     * Marks a block position
     * @param pos the pos
     */
    @Override
    public void tiquality_mark(BlockPos pos){
        tiquality_STORAGE.mark(pos);
    }

    /**
     * Unmarks a block position
     * @param pos the pos
     */
    @Override
    public void tiquality_unMark(BlockPos pos){
        tiquality_STORAGE.unMark(pos);
    }

    /**
     * Checks if a position is marked.
     * @param pos the position
     * @return marked
     */
    @Override
    public boolean tiquality_isMarked(BlockPos pos){
        return tiquality_STORAGE.isMarked(pos);
    }
}
