package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerHolder;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.List;
import java.util.Random;

public interface Tracker {

    /**
     * Use this to determine if you need to read NBT data.
     * You can also return an existing tracker.
     *
     * However, if you do return an existing tracker, update checkColission accordingly.
     * @return Tracker
     */
    Tracker load(TiqualityWorld world, NBTTagCompound nbt);

    boolean shouldSaveToDisk();

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    NBTTagCompound getNBT();

    void setProfileEnabled(boolean shouldProfile);

    @Nullable TickLogger stopProfiler();

    boolean canProfile();

    boolean isProfiling();

    void setNextTickTime(long granted_ns);

    /**
     * Gets the tick time multiplier for the TrackerBase.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    double getMultiplier(GameProfile[] cache);

    long getRemainingTime();

    boolean needsTick();

    void tickTileEntity(TiqualitySimpleTickable tileEntity);

    void tickEntity(TiqualityEntity entity);

    void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void grantTick();

    void associateChunk(TiqualityChunk chunk);
    void associateDelegatingTracker(Tracker tracker);
    void removeDelegatingTracker(Tracker tracker);

    /**
     * Gets the associated players for this tracker
     * @return a list of all players involved with this tracker.
     */
    @Nonnull
    List<GameProfile> getAssociatedPlayers();

    String toString();

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    TextComponentString getInfo();

    @Nonnull
    String getIdentifier();

    boolean shouldUnload();

    @OverridingMethodsMustInvokeSuper
    void onUnload();

    /**
     * This is called when a holder has been assigned to this tracker.
     * @param holder holder
     */
    void setHolder(TrackerHolder holder);

    /**
     * Simply return the holder you've received in setHolder.
     * @return holder
     */
    TrackerHolder getHolder();

    /**
     * Fired whenever a block is changed that is owned by this tracker
     * In general, you don't have to use this.
     *
     * @param world the world
     * @param pos the position
     * @param state the new block state
     */
    default void notifyBlockStateChange(TiqualityWorld world, BlockPos pos, IBlockState state){

    }
}
