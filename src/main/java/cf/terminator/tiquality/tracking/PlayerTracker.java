package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class PlayerTracker extends TrackerBase {

    private final GameProfile profile;

    /**
     * This method is called by Tiquality, do not instantiate these yourself!
     * This method is required, and always uses TiqualityWorld and NBTCompoundTag
     */
    public PlayerTracker(TiqualityWorld world, NBTTagCompound tag) {
        this(ForgeData.getGameProfileByUUID(new UUID(tag.getLong("uuidMost"), tag.getLong("uuidLeast"))));
    }

    /**
     * Creates the tracker
     * @param profile a given game profile
     */
    public PlayerTracker(@Nonnull GameProfile profile) {
        super();
        this.profile = profile;
    }

    /*
     * Gets the tracker for a player, if no one exists yet, it will create one. Never returns null.
     * @param profile the profile to bind this tracker to the profile MUST contain an UUID!
     * @return the associated PlayerTracker
     */
    public static @Nonnull PlayerTracker getOrCreatePlayerTrackerByProfile(@Nonnull final GameProfile profile){
        UUID id = profile.getId();
        if(id == null){
            throw new IllegalArgumentException("GameProfile must have an UUID");
        }

        PlayerTracker tracker = TrackerManager.foreach(new TrackerManager.Action<PlayerTracker>() {
            @Override
            public void each(Tracker tracker) {
                if(tracker instanceof PlayerTracker){
                    PlayerTracker playerTracker = (PlayerTracker) tracker;
                    if(playerTracker.getOwner().equals(profile)){
                        stop(playerTracker);
                    }
                }
            }
        });

        if(tracker != null){
            return tracker;
        }else {
            return TrackerManager.addTracker(TrackerHolder.getHolder(new PlayerTracker(profile))).getTracker();
        }
    }

    /**
     * Checks if the owner of this tracker is online or not.
     * @param onlinePlayerProfiles an array of online players
     * @return true if online
     */
    public boolean isPlayerOnline(final GameProfile[] onlinePlayerProfiles){
        for(GameProfile profile : onlinePlayerProfiles){
            if(this.profile.equals(profile)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("uuidMost", profile.getId().getMostSignificantBits());
        tag.setLong("uuidLeast", profile.getId().getLeastSignificantBits());
        return tag;
    }

    /**
     * Gets the tick time multiplier for the PlayerTracker.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    public double getMultiplier(final GameProfile[] cache){
        if(isPlayerOnline(cache)){
            return 1;
        }else{
            return TiqualityConfig.OFFLINE_PLAYER_TICK_TIME_MULTIPLIER;
        }
    }

    /**
     * Gets the associated player for this tracker
     * @return a list containing just 1 player.
     */
    @Override
    @Nonnull
    public List<GameProfile> getAssociatedPlayers() {
        List<GameProfile> list = new ArrayList<>();
        list.add(profile);
        return list;
    }

    /**
     * Gets the owner corresponding to this PlayerTracker.
     * @return the owner's profile
     */
    public GameProfile getOwner(){
        return profile;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    @Override
    public String toString(){
        return "PlayerTracker:{Owner: '" + getOwner().getName() + "', nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + ", hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo(){
        return new TextComponentString(TextFormatting.GREEN + "Tracked by: " + TextFormatting.AQUA + getOwner().getName());
    }

    /**
     * @return an unique identifier for this TrackerBase type, used to re-instantiate the tracker later on.
     */
    @Nonnull
    public String getIdentifier() {
        return "PlayerTracker";
    }

    @Override
    public void checkCollision(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException {
        if(this.equals(tracker)){
            throw new TrackerAlreadyExistsException(this, tracker);
        }
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof PlayerTracker == false){
            return false;
        }else{
            return o == this || this.getOwner().getId().equals(((PlayerTracker) o).getOwner().getId());
        }
    }

    @Override
    public int hashCode(){
        return getOwner().getId().hashCode();
    }
}
