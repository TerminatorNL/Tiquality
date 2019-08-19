package com.github.terminatornl.tiquality.integration.ftbutilities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.tracking.PlayerTracker;
import com.github.terminatornl.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class FTBUtilitiesHook {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
    }

    @Nullable
    public static PlayerTracker getTrackerForTeam(TiqualityWorld world, @Nonnull ForgeTeam team) {
        UUID uuid = team.owner.getId();
        GameProfile profile = ForgeData.getGameProfileByUUID(uuid);
        return profile.equals(ForgeData.GAME_PROFILE_NOBODY) ? null : PlayerTracker.getOrCreatePlayerTrackerByProfile(world, profile);
    }
}
