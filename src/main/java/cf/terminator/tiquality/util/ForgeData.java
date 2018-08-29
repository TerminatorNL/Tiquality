package cf.terminator.tiquality.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class ForgeData {

    public static final MinecraftServer SERVER = FMLCommonHandler.instance().getMinecraftServerInstance();

    public static GameProfile getGameProfileByUUID(UUID uuid){
        return SERVER.getPlayerProfileCache().getProfileByUUID(uuid);
    }
}
