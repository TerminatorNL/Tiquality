package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityMinecraftServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements TiqualityMinecraftServer {

    @Shadow private Thread serverThread;

    @Override
    public Thread getServerThread() {
        return serverThread;
    }
}
