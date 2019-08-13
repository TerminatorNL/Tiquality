package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.mixinhelper.MixinConfigPlugin;
import cf.terminator.tiquality.util.ForgetFulProgrammerException;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityLockableLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityHopper.class)
public abstract class MixinHopperlag extends TileEntityLockableLoot {

    /**
     * Used to test lag-generation, is not included in actual release.
     */
    @Inject(method = "update", at = @At("HEAD"), require = 1)
    private void dolag(CallbackInfo ci){
        if(MixinConfigPlugin.isProductionEnvironment()){
            throw new ForgetFulProgrammerException();
        }
        if (this.world.isRemote == false) {
            try {
                Thread.sleep(5, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
