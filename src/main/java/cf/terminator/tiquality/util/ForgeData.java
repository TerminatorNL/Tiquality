package cf.terminator.tiquality.util;

import cf.terminator.tiquality.Tiquality;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class ForgeData {

    public static final MinecraftServer SERVER = FMLCommonHandler.instance().getMinecraftServerInstance();

    /**
     * Gets the profile, but can reach out and contact mojang's servers if it failed.
     * "But Term! Networking on the main thread is wrong!"
     * I like to live dangerously. Besides, this beats a crash any day!
     *
     * @param uuid uuid
     * @return GameProfile
     */
    public static @Nonnull GameProfile getGameProfileByUUID(UUID uuid){

        /*
                This works 99% of the time
         */
        GameProfile profile = SERVER.getPlayerProfileCache().getProfileByUUID(uuid);
        if(profile != null){
            return profile;
        }

        Tiquality.LOGGER.warn("Player profile was not found in cache!");
        Tiquality.LOGGER.warn("I will add it, but it can cause some lag, as I may or may not contact the Mojang servers to get go and get it.");
        Tiquality.LOGGER.warn("UUID: " + uuid.toString());
        Tiquality.LOGGER.warn("Most significant bits:  " + uuid.getMostSignificantBits());
        Tiquality.LOGGER.warn("Least significant bits: " + uuid.getLeastSignificantBits());

        /*
                If it did not, we try another way, which may or may not contact mojang.
         */
        profile = new GameProfile(uuid, null);
        SERVER.getMinecraftSessionService().fillProfileProperties(profile, false);

        /*
                If it still did not, we make sure to contact mojang.
         */
        if(profile.getName() == null){
            SERVER.getMinecraftSessionService().fillProfileProperties(profile, true);
        }

        /*
                We save the result, making sure we don't have to ever do this again for this profile
         */
        SERVER.getPlayerProfileCache().addEntry(profile);
        return profile;
    }

    public static GameProfile getGameProfileByName(String name){
        return SERVER.getPlayerProfileCache().getGameProfileForUsername(name);
    }
}
