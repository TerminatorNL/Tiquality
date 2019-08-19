package com.github.terminatornl.tiquality.mixin;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.interfaces.TiqualitySimpleTickable;
import com.github.terminatornl.tiquality.interfaces.UpdateTyped;
import com.github.terminatornl.tiquality.profiling.ReferencedTickable;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(TileEntity.class)
public abstract class MixinTileEntity implements TiqualitySimpleTickable, UpdateTyped {

    @Shadow
    protected BlockPos pos;
    @Shadow
    protected World world;
    private boolean isMarkedByTiquality = false;

    @Shadow
    public abstract Block getBlockType();

    /**
     * Checks if the chunk this tickable is in is loaded
     *
     * @return chunk status
     */
    @Override
    public boolean tiquality_isLoaded() {
        return world.isBlockLoaded(pos);
    }

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void tiquality_doUpdateTick() {
        Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) this);
    }

    /**
     * Method to get the position of the object
     */
    @Override
    public BlockPos tiquality_getPos() {
        return this.pos;
    }

    /**
     * Method to get the world of the object
     */
    @Override
    public World tiquality_getWorld() {
        return this.world;
    }

    @Nullable
    @Override
    public ReferencedTickable.Reference getId() {
        return new ReferencedTickable.BlockReference(this.world.provider.getDimension(), this.pos);
    }

    @Override
    public void tiquality_mark() {
        isMarkedByTiquality = true;
    }

    @Override
    public void tiquality_unMark() {
        isMarkedByTiquality = false;
    }

    @Override
    public boolean tiquality_isMarked() {
        return isMarkedByTiquality;
    }

    @Nonnull
    @Override
    public UpdateType getUpdateType() {
        return ((UpdateTyped) this.getBlockType()).getUpdateType();
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        ((UpdateTyped) this.getBlockType()).setUpdateType(type);
    }
}
