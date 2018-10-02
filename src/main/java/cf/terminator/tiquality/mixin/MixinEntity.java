package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TickLogger;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.ForgeData;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements TiqualityEntity {

    @Shadow public abstract void onUpdate();
    @Shadow public abstract UUID getPersistentID();
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World world;
    private PlayerTracker tracker = null;

    @Override
    public void doUpdateTick() {
        this.onUpdate();
    }

    @Override
    public BlockPos getPos() {
        return new BlockPos(this.posX, this.posY, this.posZ);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public TickLogger.Location getLocation() {
        return new TickLogger.Location(this);
    }

    /**
     * Gets the type of this Tickable
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.ENTITY;
    }

    @Override
    public @Nullable PlayerTracker getPlayerTracker() {
        return tracker;
    }

    @Override
    public void setPlayerTracker(@Nullable PlayerTracker tracker) {
        this.tracker = tracker;
    }

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void TiqualityOnWrite(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir){
        if(tracker != null) {
            UUID uuid = tracker.getOwner().getId();
            NBTTagCompound tiqualityTag = new NBTTagCompound();
            tiqualityTag.setLong("uuidMost", uuid.getMostSignificantBits());
            tiqualityTag.setLong("uuidLeast", uuid.getLeastSignificantBits());
            compound.setTag("Tiquality", tiqualityTag);
        }
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void TiqualityOnRead(NBTTagCompound compound, CallbackInfo ci){
        if(compound.hasKey("Tiquality")) {
            NBTTagCompound tiqualityTag = compound.getCompoundTag("Tiquality");
            UUID uuid = new UUID(tiqualityTag.getLong("uuidMost"), tiqualityTag.getLong("uuidLeast"));
            tracker = TrackerHub.getOrCreatePlayerTrackerByProfile(ForgeData.getGameProfileByUUID(uuid));
        }
    }
}
