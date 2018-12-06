package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.interfaces.*;
import cf.terminator.tiquality.memory.WeakReferencedChunk;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerHolder;
import cf.terminator.tiquality.util.ForgeData;
import cf.terminator.tiquality.util.Utils;
import com.mojang.authlib.GameProfile;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.world.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GriefPreventionTracker implements Tracker {

    public final Claim claim;
    public final List<GameProfile> trustedPlayers = new ArrayList<>();
    public PlayerTracker ownerTracker;
    private int unloadCooldown = 40;
    private boolean scannedChunks = false;
    private final HashSet<WeakReferencedChunk> CHUNKS = new HashSet<>();

    private final LoadListener loadListener = new LoadListener();
    private final UnLoadListener unLoadListener = new UnLoadListener();

    @SuppressWarnings("unused")
    /*
     * Used by Tiquality, using reflection.
     */
    public GriefPreventionTracker(TiqualityWorld world, NBTTagCompound tag){
        this(thisMustBeFirstStatementInBodyWorkaround(world, tag));
    }

    public GriefPreventionTracker(@Nonnull Claim in){

        this.claim = in;

        updatePlayers();
        registerAsListener();

    }

    public void setOwner(UUID owner){
        this.ownerTracker = PlayerTracker.getOrCreatePlayerTrackerByProfile(ForgeData.getGameProfileByUUID(owner));
        this.ownerTracker.associateDelegatingTracker(this);
        trustedPlayers.remove(this.ownerTracker.getOwner());
    }

    public void setBlockTrackers(Runnable beforeRun, Runnable callback){
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

        world.setTrackerCuboidAsync(startPos, endPos, this, callback, beforeRun);
    }

    private void registerAsListener(){
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, LoadChunkEvent.class, loadListener);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, UnloadChunkEvent.class, unLoadListener);
    }

    private void unRegisterAsListener(){
        Sponge.getEventManager().unregisterListeners(loadListener);
        Sponge.getEventManager().unregisterListeners(unLoadListener);
    }

    private class LoadListener implements EventListener<LoadChunkEvent>{
        @Override
        public void handle(@Nonnull LoadChunkEvent event) {
            TiqualityChunk chunk = (TiqualityChunk) event.getTargetChunk();

            Location<org.spongepowered.api.world.World> startLoc = claim.getGreaterBoundaryCorner();
            BlockPos startPos = new BlockPos(
                    startLoc.getBlockX(),
                    startLoc.getBlockY(),
                    startLoc.getBlockZ()
            );

            Location<org.spongepowered.api.world.World> endLoc = claim.getGreaterBoundaryCorner();
            BlockPos endPos = new BlockPos(
                    endLoc.getBlockX(),
                    endLoc.getBlockY(),
                    endLoc.getBlockZ()
            );

            synchronized (CHUNKS) {
                if (Utils.Chunk.isWithinBounds(chunk.getMinecraftChunk().getPos(), startPos, endPos)) {
                    CHUNKS.add(new WeakReferencedChunk((TiqualityChunk) event.getTargetChunk()));
                }
            }

        }
    }

    private class UnLoadListener implements EventListener<UnloadChunkEvent>{
        @Override
        public void handle(@Nonnull UnloadChunkEvent event) {
            TiqualityChunk chunk = (TiqualityChunk) event.getTargetChunk();

            Location<org.spongepowered.api.world.World> startLoc = claim.getGreaterBoundaryCorner();
            BlockPos startPos = new BlockPos(
                    startLoc.getBlockX(),
                    startLoc.getBlockY(),
                    startLoc.getBlockZ()
            );

            Location<org.spongepowered.api.world.World> endLoc = claim.getGreaterBoundaryCorner();
            BlockPos endPos = new BlockPos(
                    endLoc.getBlockX(),
                    endLoc.getBlockY(),
                    endLoc.getBlockZ()
            );

            synchronized (CHUNKS) {
                if (Utils.Chunk.isWithinBounds(chunk.getMinecraftChunk().getPos(), startPos, endPos)) {
                    CHUNKS.remove(new WeakReferencedChunk((TiqualityChunk) event.getTargetChunk()));
                }
            }

        }
    }

    /**
     * Because java is incompetent.
     * @param world .
     * @param tag .
     * @return .
     */
    private static Claim thisMustBeFirstStatementInBodyWorkaround(TiqualityWorld world, NBTTagCompound tag){
        long least = tag.getLong("uuidLeast");
        long most = tag.getLong("uuidMost");
        UUID claim_uuid = new UUID(most, least);
        org.spongepowered.api.world.World spongeWorld = (org.spongepowered.api.world.World) world;
        Optional<Claim> result = GriefPrevention.getApi().getClaimManager(spongeWorld).getClaimByUUID(claim_uuid);
        if(result.isPresent() == false){
            //return null;
            throw new IllegalStateException();
        }else{
            return result.get();
        }
    }

    public void replaceTracker(Tracker tracker){
        if(claim == null){
            return;
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

        world.setTrackerCuboidAsync(startPos, endPos, tracker, null);
        for(Claim subClaim : claim.getChildren(false)){
            world.setTrackerCuboidAsync(startPos, endPos, GriefPreventionHook.findOrGetTrackerByClaim(subClaim), null);
        }

        for(TiqualityEntity entity : world.getEntities(true)){
            if(entity.getTracker() == this){
                entity.setTracker(null);
            }
        }
        CHUNKS.clear();
    }

    public void updatePlayers(){
        if(doesClaimExists() == false){
            return;
        }
        trustedPlayers.clear();
        List<UUID> trustees = new ArrayList<>();
        if(claim.getData() != null){
            trustees.addAll(claim.getUserTrusts());
        }
        for (UUID uuid : trustees) {
            trustedPlayers.add(ForgeData.getGameProfileByUUID(uuid));
        }
        this.ownerTracker = PlayerTracker.getOrCreatePlayerTrackerByProfile(ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId()));
        this.ownerTracker.associateDelegatingTracker(this);
    }

    @Override
    public void onUnload(){
        unRegisterAsListener();
        if(doesClaimExists() == false && ownerTracker != null){
            replaceTracker(ownerTracker);
        }
    }

    public boolean hasLoadedChunks(){
        synchronized (CHUNKS) {
            CHUNKS.removeIf(chunk -> chunk.isChunkLoaded() == false);
            return CHUNKS.size() > 0;
        }
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        return ownerTracker.compareTo(o);
    }

    @Override
    public void checkColission(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException {

    }

    private TrackerHolder holder = null;

    @Override
    public void setHolder(TrackerHolder holder) {
        this.holder = holder;
    }

    @Override
    public TrackerHolder getHolder() {
        return holder;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof GriefPreventionTracker == false){
            return false;
        }else if(this == o){
            return true;
        }
        GriefPreventionTracker other = (GriefPreventionTracker) o;
        return this.claim.equals(other.claim);
    }

    @Override
    public boolean shouldUnload(){
        return hasLoadedChunks() == false && unloadCooldown == 0;
    }

    /**
     * Does the claim still exist?
     * @return true if it exists.
     */
    public boolean doesClaimExists(){
        return claim != null && GriefPrevention.getApi().getClaimManager(claim.getWorld()).getClaimByUUID(claim.getUniqueId()).isPresent();
    }

    @Override
    public boolean shouldSaveToDisk(){
        return doesClaimExists();
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        UUID claim_uuid = claim.getUniqueId();
        tag.setLong("uuidLeast", claim_uuid.getLeastSignificantBits());
        tag.setLong("uuidMost", claim_uuid.getMostSignificantBits());
        return tag;
    }

    @Override
    public TickLogger getTickLogger() {
        return null;
    }

    /**
     * We delegate all calls to PlayerTracker.
     * @param cache The current online player cache
     * @return the multiplier
     */
    @Override
    public double getMultiplier(GameProfile[] cache) {
        return 0;
    }

    @Override
    public long getRemainingTime() {
        return 0;
    }

    /**
     * @return an unique identifier for this TrackerBase CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier(){
        return "GPClaim";
    }

    /**
     * Gets the associated players for this tracker
     *
     * @return a list of all players involved with this tracker.
     */
    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        List<GameProfile> list = new ArrayList<>(trustedPlayers);
        list.add(ownerTracker.getOwner());
        return list;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    public void setProfileEnabled(boolean shouldProfile){
        ownerTracker.setProfileEnabled(shouldProfile);
    }

    @Override
    public @Nullable TickLogger stopProfiler(){
        return ownerTracker.stopProfiler();
    }

    @Override
    public void setNextTickTime(long granted_ns) {
        if(unloadCooldown > 0){
            unloadCooldown--;
        }
        if(unloadCooldown == 1 && scannedChunks == false){
            scannedChunks = true;

            int low_x = claim.getLesserBoundaryCorner().getBlockX();
            int low_z = claim.getLesserBoundaryCorner().getBlockZ();

            int high_x = claim.getGreaterBoundaryCorner().getBlockX();
            int high_z = claim.getGreaterBoundaryCorner().getBlockZ();

            IChunkProvider provider = ((TiqualityWorld) claim.getWorld()).getMinecraftChunkProvider();

            for (int x = low_x; x <= high_x + 16; x = x + 16) {
                for (int z = low_z; z <= high_z + 16; z = z + 16) {
                    TiqualityChunk chunk = (TiqualityChunk) provider.getLoadedChunk(x >> 4, z >> 4);
                    if(chunk != null) {
                        CHUNKS.add(new WeakReferencedChunk(chunk));
                    }
                }
            }
        }
    }

    @Override
    public void tickTileEntity(TiqualitySimpleTickable t){
        ownerTracker.tickTileEntity(t);
    }

    @Override
    public void tickEntity(TiqualityEntity e){
        ownerTracker.tickEntity(e);
    }

    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        ownerTracker.doBlockTick(block, world, pos, state, rand);
    }

    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        ownerTracker.doRandomBlockTick(block, world, pos, state, rand);
    }

    @Override
    public void grantTick() {

    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {

    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException("This tracker already is a delegator!");
    }

    @Override
    public boolean isLoaded() {
        return hasLoadedChunks() || unloadCooldown > 0;
    }

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo() {
        if (doesClaimExists() == false) {
            return new TextComponentString(TextFormatting.GREEN + "GP-claim: " + TextFormatting.RED + "deleted.");
        } else {
            return new TextComponentString(TextFormatting.GREEN + "GP-claim: " + TextFormatting.WHITE + ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId()).getName());
        }
    }


    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    public String toString(){

        String claimText = claim == null ? "deleted" : claim.getLesserBoundaryCorner().toString();
        String ownerText = ownerTracker == null ? "unknown" : ownerTracker.getOwner().getName();

        return "GP-TrackerBase:{hashCode: " + System.identityHashCode(this) + "owner: " + ownerText + ", claim: " + claimText + "delegated: " + ownerTracker.toString() + "}";
    }
}
