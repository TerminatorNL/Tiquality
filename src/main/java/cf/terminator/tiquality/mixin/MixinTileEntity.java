package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntity.class)
public class MixinTileEntity implements TiqualitySimpleTickable {

    @Shadow protected BlockPos pos;
    @Shadow protected World world;
    private boolean isMarkedByTiquality = false;

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {
        Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) this);
    }

    /**
     * Method to get the position of the object
     */
    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    /**
     * Method to get the world of the object
     */
    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public TickLogger.Location getLocation() {
        return new TickLogger.Location(this.world, this.pos);
    }

    /**
     * Gets the type of this Tickable
     *
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.TILE_ENTITY;
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
}
