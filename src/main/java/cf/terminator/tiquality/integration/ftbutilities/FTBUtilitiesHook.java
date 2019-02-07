package cf.terminator.tiquality.integration.ftbutilities;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.util.ForgeData;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class FTBUtilitiesHook {

    public static void init(){
        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
    }

    @Nullable
    public static PlayerTracker getTrackerForTeam(TiqualityWorld world, @Nonnull ForgeTeam team){
        UUID uuid = team.owner.getId();
        GameProfile profile = ForgeData.getGameProfileByUUID(uuid);
        PlayerTracker tracker;
        if(profile.equals(ForgeData.GAME_PROFILE_NOBODY)){
            tracker = null;
        }else {
            tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile(world, profile);
        }
        return tracker;
    }
}
