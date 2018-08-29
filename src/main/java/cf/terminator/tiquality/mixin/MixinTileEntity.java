package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntity.class)
public class MixinTileEntity implements TiqualitySimpleTickable {
    /**
     * Method to actually run the reloadFromFile on the tickable.
     */
    @Override
    public void doUpdateTick() {
        ((ITickable) this).update();
    }
}
