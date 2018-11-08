package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.mixinhelper.MixinConfigPlugin;
import cf.terminator.tiquality.util.ForgetFulProgrammerException;
import net.minecraft.tileentity.TileEntityHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityHopper.class)
public class MixinHopperlag {

    /**
     * Used to test lag-generation, is not included in actual release.
     */
    @Inject(method = "update", at = @At("HEAD"))
    private void dolag(CallbackInfo ci){
        if(MixinConfigPlugin.isProductionEnvironment()){
            throw new ForgetFulProgrammerException();
        }
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
