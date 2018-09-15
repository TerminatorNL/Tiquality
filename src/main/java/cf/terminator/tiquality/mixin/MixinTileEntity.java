package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.store.TickLogger;
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

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {
        ((ITickable) this).update();
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
}
