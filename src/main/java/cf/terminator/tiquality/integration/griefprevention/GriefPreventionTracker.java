package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.TrackerBase;
import cf.terminator.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.api.entity.living.player.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GriefPreventionTracker extends TrackerBase {

    public final Claim claim;
    public final List<GameProfile> trustedPlayers = new ArrayList<>();
    public GameProfile owner;

    @SuppressWarnings("unused")
    /*
     * Used by Tiquality, using reflection.
     */
    public GriefPreventionTracker(TiqualityWorld world, NBTTagCompound tag){
        this(thisMustBeFirstStatementInBodyWorkaround(world, tag));
    }

    public GriefPreventionTracker(Claim in){
        this.claim = in;
        updatePlayers();
    }

    public void setOwner(UUID owner){
        this.owner = ForgeData.getGameProfileByUUID(owner);
        trustedPlayers.remove(this.owner);
    }

    public void setBlockTrackers(Runnable callback){
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

        world.setTrackerCuboidAsync(startPos, endPos, this, callback);
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
            return null;
        }else{
            return result.get();
        }
    }

    public void replaceTracker(TrackerBase tracker){
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
        owner = ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId());
    }

    @Override
    public void onUnload(){
        if(owner != null && doesClaimExists() == false){
            replaceTracker(PlayerTracker.getOrCreatePlayerTrackerByProfile(owner));
        }
    }

    @Override
    public boolean forceUnload(){
        return doesClaimExists() == false;
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

    /**
     * If the owner is online, return 1 divided by the amount of claims the owner has
     * If a trusted player is inside the claim, return 1 divided by the amount of claims the owner has
     *
     * Otherwise, return the offline player time multiplier divided by the amount of claims the owner has
     *
     * @param cache The current online player cache
     * @return the multiplier
     */
    @Override
    public double getMultiplier(GameProfile[] cache) {
        if(doesClaimExists() == false || owner == null){
            return 0;
        }
        double claimcount = claim.getClaimManager().getPlayerClaims(owner.getId()).size();
        if(claimcount == 0) {
            return 0;
        }
        for(GameProfile profile : cache){
            if(profile.equals(owner)){
                return 1/claimcount;
            }else if(trustedPlayers.contains(profile)){
                for(Player player : claim.getPlayers()){
                    if(player.getProfile().getUniqueId().equals(profile.getId())){
                        return 1/claimcount;
                    }
                }
            }
        }
        return TiqualityConfig.OFFLINE_PLAYER_TICK_TIME_MULTIPLIER/claimcount;
    }

    /**
     * @return an unique identifier for this Tracker CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier(){
        return "MixinClaim";
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
        list.add(owner);
        return list;
    }

    /**
     * @return the info describing this Tracker (Like the owner)
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

        String claimText = claim == null ? "deleted" : claim.getGreaterBoundaryCorner().toString();
        String ownerText = owner == null ? "unknown" : owner.getName();

        return "GP-Tracker:{nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + ", hashCode: " + System.identityHashCode(this) + ", claim: " + claimText + "owner: " + ownerText + "}";
    }
}
