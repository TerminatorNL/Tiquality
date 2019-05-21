package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.*;

import static cf.terminator.tiquality.Tiquality.PREFIX;
import static cf.terminator.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

@SuppressWarnings("WeakerAccess")
public class PlayerTracker extends TrackerBase {

    private final GameProfile profile;
    private boolean notifyUser = true;
    private long nextMessageMillis = 0L;
    private final Set<Long> sharedTo = new HashSet<>();
    private TickWallet wallet = new TickWallet();

    /**
     * Required.
     */
    public PlayerTracker(){
        profile = ForgeData.GAME_PROFILE_NOBODY;
    }

    public List<TextComponentString> getSharedToTextual(TiqualityWorld world){
        LinkedList<TextComponentString> list = new LinkedList<>();
        for(long id : sharedTo){
            TrackerHolder holder = TrackerHolder.getTrackerHolder(world, id);
            if(holder == null || holder.getTracker() instanceof PlayerTracker == false){
                switchSharedTo(id);
                list.add(new TextComponentString(TextFormatting.RED + "Tracker ID: " + id + " removed!"));
            }else{
                list.add(new TextComponentString(TextFormatting.WHITE + ((PlayerTracker) holder.getTracker()).getOwner().getName()));
            }
        }
        return list;
    }

    public boolean switchNotify(){
        notifyUser = notifyUser == false;
        return notifyUser;
    }

    public boolean switchSharedTo(long id){
        if(sharedTo.contains(id) == false){
            sharedTo.add(id);
            getHolder().update();
            return true;
        }else{
            sharedTo.remove(id);
            getHolder().update();
            return false;
        }
    }

    public void addWallet(TickWallet wallet){
        this.wallet.addWallet(wallet);
    }

    public TickWallet getWallet(){
        return this.wallet;
    }

    @Override
    public void setNextTickTime(long time){
        super.setNextTickTime(time);
        wallet.clearWallets();
        wallet.setRemainingTime(time);
    }

    @Override
    public void tick(){
        super.tick();
        for(Long id : sharedTo){
            Tracker tracker = TrackerManager.getTrackerByID(id);
            if(tracker instanceof PlayerTracker){
                ((PlayerTracker) tracker).addWallet(this.wallet);
            }
        }
    }

    /**
     * Notify this tracker about it's performance falling behind.
     * @param ratio the tracker's speed compared to the server tick time.
     */
    public void notifyFallingBehind(double ratio) {
        if(notifyUser && System.currentTimeMillis() > nextMessageMillis){
            nextMessageMillis = System.currentTimeMillis() + (TiqualityConfig.DEFAULT_THROTTLE_WARNING_INTERVAL_SECONDS * 1000);
            Entity e = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(getOwner().getId());
            if(e instanceof EntityPlayer){
                EntityPlayer player = (EntityPlayer) e;
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Warning: " + TextFormatting.GRAY + "Your blocks tick at " + (Math.round(ratio * 10000D)/100D) + "% speed." + TextFormatting.DARK_GRAY + " (/tq notify)"), true);
                double serverTPS_raw = Tiquality.TPS_MONITOR.getAverageTPS();

                String serverTPS = TWO_DECIMAL_FORMATTER.format(Math.round(serverTPS_raw * 100D) / 100D);
                String playerTPS = TWO_DECIMAL_FORMATTER.format(Math.round(serverTPS_raw * ratio * 100D)/100D);



                player.sendMessage(new TextComponentString(PREFIX + "Your TPS: " + TextFormatting.WHITE + playerTPS + TextFormatting.GRAY + " (" + TextFormatting.WHITE + Math.round(ratio * 100D) + "%" + TextFormatting.GRAY  + ")" + TextFormatting.DARK_GRAY + " (/tq notify)"));
            }
        }
    }

    /**
     * Decreases the remaining tick time for a tracker.
     * @param time in nanoseconds
     */
    @Override
    public void consume(long time){
        wallet.consume(time);
    }

    @Override
    public long getRemainingTime(){
        return wallet.getTimeLeft();
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound trackerTag) {
        PlayerTracker tracker = new PlayerTracker(ForgeData.getGameProfileByUUID(new UUID(trackerTag.getLong("uuidMost"), trackerTag.getLong("uuidLeast"))));
        if(trackerTag.hasKey("shared")){
            NBTTagList shared = trackerTag.getTagList("shared", 4);
            for(NBTBase base : shared){
                tracker.sharedTo.add(((NBTTagLong) base).getLong());
            }
        }
        tracker.notifyUser = trackerTag.getBoolean("notify");
        return tracker;
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
    @Nonnull
    public static PlayerTracker getOrCreatePlayerTrackerByProfile(TiqualityWorld world, @Nonnull final GameProfile profile){
        UUID id = profile.getId();
        if(id == null){
            throw new IllegalArgumentException("GameProfile must have an UUID");
        }

        PlayerTracker tracker = TrackerManager.foreach(new TrackerManager.Action<PlayerTracker>() {
            @Override
            public void each(Tracker tracker) {
                if(tracker instanceof PlayerTracker){
                    PlayerTracker playerTracker = (PlayerTracker) tracker;
                    if(playerTracker.getOwner().getId().equals(id)){
                        stop(playerTracker);
                    }
                }
            }
        });

        if(tracker != null){
            return tracker;
        }else {
            return TrackerManager.createNewTrackerHolder(world, new PlayerTracker(profile)).getTracker();
        }
    }

    /**
     * Checks if the owner of this tracker is online or not.
     * @param onlinePlayerProfiles an array of online players
     * @return true if online
     */
    public boolean isPlayerOnline(final GameProfile[] onlinePlayerProfiles){
        for(GameProfile profile : onlinePlayerProfiles){
            if(this.profile.getId().equals(profile.getId())){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Nonnull
    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("uuidMost", profile.getId().getMostSignificantBits());
        tag.setLong("uuidLeast", profile.getId().getLeastSignificantBits());
        tag.setBoolean("notify",notifyUser);
        if(sharedTo.size() > 0) {
            NBTTagList sharedToTag = new NBTTagList();
            for(long id : sharedTo){
                sharedToTag.appendTag(new NBTTagLong(id));
            }
            tag.setTag("shared", sharedToTag);
        }
        /* Human readable names and UUID's. These are not accessed.*/
        tag.setString("name", profile.getName());
        tag.setString("uuid", profile.getId().toString());
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
        return "PlayerTracker:{Owner: '" + getOwner().getName() + "', nsleft: " + tick_time_remaining_ns + ", unticked: " + tickQueue.size() + ", hashCode: " + System.identityHashCode(this) + "}";
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

    /**
     * Required to check for colission with unloaded trackers.
     *
     * @return int the hash code, just like Object#hashCode().
     */
    @Override
    public int getHashCode() {
        return getOwner().getId().hashCode();
    }

    /**
     * Checks if the tracker is equal to one already in the database.
     * Allows for flexibility for loading.
     *
     * @param tag tag
     * @return equals
     */
    @Override
    public boolean equalsSaved(NBTTagCompound tag) {
        UUID ownerUUID = new UUID(tag.getLong("uuidMost"), tag.getLong("uuidLeast"));
        return ownerUUID.equals(getOwner().getId());
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
