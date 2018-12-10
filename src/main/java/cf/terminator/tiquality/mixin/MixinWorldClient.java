package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.world.SpongeChunkLoader;
import cf.terminator.tiquality.world.WorldHelper;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(value = WorldClient.class, priority = 999)
public abstract class MixinWorldClient extends World implements TiqualityWorld {

    @Shadow public abstract ChunkProviderClient getChunkProvider();

    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
        throw new RuntimeException("This should never run...");
    }

    /**
     * Optimized way of getting a chunk using a BlockPos
     * @param pos the position of the block
     * @return the chunk
     */
    public @Nonnull TiqualityChunk getTiqualityChunk(BlockPos pos){
        if(Tiquality.SPONGE_IS_PRESENT){
            return SpongeChunkLoader.getChunkForced(this, pos);
        }else {
            return (TiqualityChunk) chunkProvider.provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    /**
     * Optimized way of getting the TrackerBase using a BlockPos.
     * Don't forget TrackerBase reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the chunk
     */
    public @Nullable
    Tracker getTiqualityTracker(BlockPos pos){
        return getTiqualityChunk(pos).tiquality_findTrackerByBlockPos(pos);
    }

    /**
     * Optimized way of setting the TrackerBase using a BlockPos.
     * Don't forget TrackerBase reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the TrackerBase to set.
     */
    public void setTiqualityTracker(BlockPos pos, Tracker tracker){
        getTiqualityChunk(pos).tiquality_setTrackedPosition(pos, tracker);
    }

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All higher)
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in the main thread!
     */
    public void setTiqualityTrackerCuboidAsync(BlockPos start, BlockPos end, Tracker tracker, Runnable callback){
        WorldHelper.setTrackerCuboid(this, start, end, tracker, callback, null);
    }

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All higher)
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in the main thread!
     * @param beforeRun a task to run before work starts
     */
    public void setTiqualityTrackerCuboidAsync(BlockPos start, BlockPos end, Tracker tracker, Runnable callback, Runnable beforeRun){
        WorldHelper.setTrackerCuboid(this, start, end, tracker, callback, beforeRun);
    }

    /**
     * Gets the minecraft world
     * @return the chunk
     */
    @Nonnull
    public World getMinecraftWorld(){
        return this;
    }

    /**
     * Gets all entities in this world
     * @param trackersOnly set this to true if you're only intrested in entities which have a tracker associated.
     *                    If this is true, you are also able to edit the list. If this is false, you are returned an unmodifiable list
     * @return a list of entities, or an empty list if there are none
     */
    @Nonnull
    public List<TiqualityEntity> getTiqualityEntities(boolean trackersOnly){
        if(trackersOnly){
            //noinspection unchecked
            List<TiqualityEntity> list = (List<TiqualityEntity>) (Object)  new ArrayList<>(loadedEntityList);
            list.removeIf(entity -> entity.getTracker() == null);
            return list;
        }else{
            //noinspection unchecked
            return (List<TiqualityEntity>) (Object) Collections.unmodifiableList(loadedEntityList);
        }
    }

    @Override
    public ChunkProviderClient getMinecraftChunkProvider(){
        return getChunkProvider();
    }
}
