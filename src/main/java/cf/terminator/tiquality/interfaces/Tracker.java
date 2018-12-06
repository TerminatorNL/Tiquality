package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
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

public interface Tracker extends Comparable {

    boolean shouldSaveToDisk();

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    NBTTagCompound getNBT();

    TickLogger getTickLogger();

    void setProfileEnabled(boolean shouldProfile);

    @Nullable TickLogger stopProfiler();

    void setNextTickTime(long granted_ns);

    /**
     * Gets the tick time multiplier for the TrackerBase.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    double getMultiplier(GameProfile[] cache);

    long getRemainingTime();

    void tickTileEntity(TiqualitySimpleTickable tickable);

    void tickEntity(TiqualityEntity entity);

    void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void grantTick();

    void associateChunk(TiqualityChunk chunk);
    void associateDelegatingTracker(Tracker tracker);

    boolean isLoaded();

    /**
     * Gets the associated players for this tracker
     * @return a list of all players involved with this tracker.
     */
    @Nonnull
    List<GameProfile> getAssociatedPlayers();

    boolean isDone();

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

    @Override
    int compareTo(@Nonnull Object o);

    /**
     * Use this to throw an error if the tracker already exists, indicative of programming errors.
     */
    void checkColission(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException;

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
}
