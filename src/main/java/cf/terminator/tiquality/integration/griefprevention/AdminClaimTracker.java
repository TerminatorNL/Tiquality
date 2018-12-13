package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.*;
import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerHolder;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.api.Sponge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AdminClaimTracker extends GriefPreventionTracker {

    public static final AdminClaimTracker INSTANCE = new AdminClaimTracker();
    private static TrackerHolder HOLDER = TrackerHolder.getHolder(INSTANCE);
    private boolean isProfiling = false;
    private TickLogger tickLogger = new TickLogger();

    /**
     * Required
     */
    public AdminClaimTracker() {
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Override
    public NBTTagCompound getNBT() {
        return new NBTTagCompound();
    }

    @Override
    public TickLogger getTickLogger() {
        return tickLogger;
    }

    @Override
    public void setProfileEnabled(boolean shouldProfile) {
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                if(isProfiling != shouldProfile) {
                    isProfiling = shouldProfile;
                    if(shouldProfile == false){
                        MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(AdminClaimTracker.this, getTickLogger()));
                    }else{
                        tickLogger.reset();
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public TickLogger stopProfiler() {
        return SynchronizedAction.run(new SynchronizedAction.Action<TickLogger>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<TickLogger> variable) {
                if(isProfiling == true) {
                    isProfiling = false;
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(AdminClaimTracker.this, getTickLogger()));
                    variable.set(getTickLogger());
                }
            }
        });
    }

    private boolean IMPORTING = false;
    @Override
    public void setBlockTrackers(Runnable runnable, Runnable r2){
        if(IMPORTING == true){
            return;
        }
        IMPORTING = true;
        List<Claim> list = new ArrayList<>();
        for (org.spongepowered.api.world.World world : Sponge.getServer().getWorlds()) {
            list.addAll(GriefPrevention.getApi().getClaimManager(world).getWorldClaims());
        }
        for(Claim claim : list) {
            if(claim.isAdminClaim() == false){
                continue;
            }
            BlockPos startPos = new BlockPos(
                    claim.getLesserBoundaryCorner().getBlockX(),
                    claim.getLesserBoundaryCorner().getBlockY(),
                    claim.getLesserBoundaryCorner().getBlockZ()
            );

            BlockPos endPos = new BlockPos(
                    claim.getGreaterBoundaryCorner().getBlockX(),
                    claim.getGreaterBoundaryCorner().getBlockY(),
                    claim.getGreaterBoundaryCorner().getBlockZ()
            );
            TiqualityWorld world = (TiqualityWorld) claim.getWorld();

            world.setTiqualityTrackerCuboidAsync(startPos, endPos, this, null, null);
            for(Claim subClaim : claim.getChildren(false)){
                if(GriefPreventionHook.isValidClaim(subClaim) == false){
                    continue;
                }
                GriefPreventionHook.findOrGetTrackerByClaim(subClaim).setBlockTrackers(null, null);
            }
        }
        IMPORTING = false;
    }

    @Override
    public void setNextTickTime(long granted_ns) {
        tickLogger.addTick(granted_ns);
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound nbt) {
        return INSTANCE;
    }

    @Override
    public boolean shouldSaveToDisk(){
        return true;
    }

    /**
     * Ticks the tile entity, and optionally profiles it.
     * @param tickable the TiqualitySimpleTickable object (Tile Entities are castable.)
     */
    @Override
    public void tickTileEntity(TiqualitySimpleTickable tickable){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) tickable);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(tickable.getLocation(), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) tickable);
        }
    }

    /**
     * Performs block tick, and optionally profiles it
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
        }
    }

    /**
     * Performs a random block tick, and optionally profiles it.
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
        }
    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException("AdminClaimTracker does not need ticks");
    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {

    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException("AdminClaimTracker should not be delegated");
    }

    @Override
    public void removeDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the associated players for this tracker
     *
     * @return an empty list
     */
    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        return Collections.emptyList();
    }

    /**
     * Ticks the entity, and optionally profiles it
     * @param entity the Entity to tick
     */
    @Override
    public void tickEntity(TiqualityEntity entity){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onEntityTick((Entity) entity);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(entity.getLocation(), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onEntityTick((Entity) entity);
        }
    }

    /**
     * Since we're a TrackerBase without an owner, we assign 0 time to it's tick time.
     * @param cache The current online player cache
     * @return 0
     */
    @Override
    public double getMultiplier(final GameProfile[] cache){
        return 0;
    }

    @Override
    public long getRemainingTime() {
        return 0;
    }

    @Override
    public boolean needsTick() {
        return false;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    @Override
    public String toString(){
        return "AdminClaimTracker:{hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.LIGHT_PURPLE + "Admin claim");
    }

    /**
     * @return an unique identifier for this TrackerBase CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier() {
        return "GPAdmin";
    }

    /**
     * We never unload.
     * @return
     */
    @Override
    public boolean shouldUnload() {
        return false;
    }

    @Override
    public void onUnload() {
        throw new UnsupportedOperationException("Unloading AdminClaimTracker is never allowed.");
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        return 0;
    }

    @Override
    public void checkCollision(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException {

    }

    @Override
    public void setHolder(TrackerHolder holder) {
        HOLDER = holder;
    }

    @Override
    public TrackerHolder getHolder() {
        return HOLDER;
    }

    @Override
    public boolean equals(Object o){
        if(o == null || o instanceof AdminClaimTracker == false){
            return false;
        }else{
            if(o != this){
                throw new IllegalStateException("Detected two AdminClaimTracker objects, this is impossible. HALT");
            }else{
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
