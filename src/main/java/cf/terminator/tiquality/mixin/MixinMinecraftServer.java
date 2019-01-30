package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityMinecraftServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements TiqualityMinecraftServer {

    @Shadow private Thread serverThread;

    @Override
    public Thread getServerThread() {
        return serverThread;
    }

    @Inject(method = "updateTimeLightAndEntities", at = @At("RETURN"))
    private void afterTick(CallbackInfo ci){

    }
}
