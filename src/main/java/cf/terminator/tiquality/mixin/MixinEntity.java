package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.profiling.ReferencedTickable;
import cf.terminator.tiquality.tracking.TrackerHolder;
import cf.terminator.tiquality.tracking.TrackerManager;
import cf.terminator.tiquality.tracking.UpdateType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements TiqualityEntity {

    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World world;
    @Shadow public abstract UUID getUniqueID();

    private TrackerHolder trackerHolder = null;
    private boolean isMarkedByTiquality = false;
    private UpdateType updateType = UpdateType.DEFAULT;

    @Override
    public void tiquality_doUpdateTick() {
        Tiquality.TICK_EXECUTOR.onEntityTick((Entity) (Object) this);
    }

    @Override
    public BlockPos tiquality_getPos() {
        return new BlockPos(this.posX, this.posY, this.posZ);
    }

    @Override
    public World tiquality_getWorld() {
        return this.world;
    }

    @Override
    @Nullable
    public ReferencedTickable.Reference getId() {
        return new ReferencedTickable.EntityReference(world.provider.getDimension(), this.getUniqueID());
    }

    @Override
    @Nullable
    public TrackerHolder getTrackerHolder() {
        return trackerHolder;
    }

    @Override
    @Nullable
    public Tracker getTracker() {
        return trackerHolder == null ? null : trackerHolder.getTracker();
    }

    @Override
    public void setTrackerHolder(@Nullable TrackerHolder trackerHolder) {
        TiqualityEvent.SetEntityTrackerEvent event = new TiqualityEvent.SetEntityTrackerEvent(this, trackerHolder);
        if(MinecraftForge.EVENT_BUS.post(event) /* is cancelled */){
            return;
        }
        this.trackerHolder = event.getHolder();
    }

    @Override
    public void setTracker(@Nullable Tracker tracker) {
        this.setTrackerHolder(tracker == null ? null : tracker.getHolder());
    }

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void TiqualityOnWrite(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir){
        if(trackerHolder != null && trackerHolder.getTracker().shouldSaveToDisk() == true) {
            compound.setTag("Tiquality", trackerHolder.getHolderTag());
        }
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void TiqualityOnRead(NBTTagCompound compound, CallbackInfo ci){
        if(compound.hasKey("Tiquality")) {
            trackerHolder = TrackerManager.readHolder((TiqualityWorld) world, compound.getCompoundTag("Tiquality"));
        }
    }


    /**
     * Checks if this tickable is loaded, eg: chunk load status
     * @return chunk status
     */
    @Override
    public boolean tiquality_isLoaded(){
        return this.world.isBlockLoaded(tiquality_getPos());
    }

    /**
     * Marks this entity
     */
    @Override
    public void tiquality_mark(){
        isMarkedByTiquality = true;
    }

    /**
     * Unmarks this entity
     */
    @Override
    public void tiquality_unMark(){
        isMarkedByTiquality = false;
    }

    /**
     * Checks if this entity is marked
     * @return marked
     */
    @Override
    public boolean tiquality_isMarked() {
        return isMarkedByTiquality;
    }


    public void setUpdateType(@Nonnull UpdateType type){
        updateType = type;
    }

    @Nonnull
    public UpdateType getUpdateType(){
        return updateType;
    }
}
