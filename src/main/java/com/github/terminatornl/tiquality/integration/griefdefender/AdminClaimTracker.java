package com.github.terminatornl.tiquality.integration.griefdefender;

import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.tracking.ForcedTracker;
import com.github.terminatornl.tiquality.tracking.TrackerHolder;
import com.github.terminatornl.tiquality.tracking.TrackerManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class AdminClaimTracker extends ForcedTracker {

    public static final AdminClaimTracker INSTANCE = new AdminClaimTracker();
    private TrackerHolder HOLDER;
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
    public boolean shouldSaveToDisk() {
        return true;
    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException("AdminClaimTracker does not need ticks");
    }

    /**
     * Since we're a TrackerBase without an owner, we assign 0 time to it's tick time.
     *
     * @param cache The current online player cache
     * @return 0
     */
    @Override
    public double getMultiplier(final GameProfile[] cache) {
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
     *
     * @return description
     */
    @Override
    public String toString() {
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
     *
     * @return false
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
    public TrackerHolder getHolder() {
        if (HOLDER == null && isGeneratingHolder == false) {
            isGeneratingHolder = true;
            HOLDER = TrackerManager.createNewTrackerHolder(null, this);
            isGeneratingHolder = false;
        }
        return HOLDER;
    }

    @Override
    public void setHolder(TrackerHolder holder) {
        HOLDER = holder;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AdminClaimTracker == false) {
            return false;
        } else {
            if (o == this) {
                return true;
            } else {
                throw new IllegalStateException("Detected two AdminClaimTracker instances, this is impossible. HALT");
            }
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
