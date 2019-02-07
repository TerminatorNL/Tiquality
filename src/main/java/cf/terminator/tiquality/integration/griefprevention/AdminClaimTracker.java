package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.ForcedTracker;
import cf.terminator.tiquality.tracking.TrackerHolder;
import cf.terminator.tiquality.tracking.TrackerManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class AdminClaimTracker extends ForcedTracker {

    public static final AdminClaimTracker INSTANCE = new AdminClaimTracker();
    private TrackerHolder<AdminClaimTracker> HOLDER;
    private boolean isGeneratingHolder = false;

    /**
     * Required
     */
    public AdminClaimTracker() {
        super();
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Nonnull
    @Override
    public NBTTagCompound getNBT() {
        return new NBTTagCompound();
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound trackerTag) {
        return INSTANCE;
    }

    @Override
    public boolean shouldSaveToDisk(){
        return true;
    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException("AdminClaimTracker does not need ticks");
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
    public void setHolder(TrackerHolder holder) {
        HOLDER = holder;
    }

    @Override
    public TrackerHolder getHolder() {
        if(HOLDER == null && isGeneratingHolder == false) {
            isGeneratingHolder = true;
            HOLDER = TrackerManager.createNewTrackerHolder(null, this);
            isGeneratingHolder = false;
        }
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
