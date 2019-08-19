package com.github.terminatornl.tiquality.util;

import com.mojang.authlib.GameProfile;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.GameProfileManager;

import java.util.UUID;

public class SpongeData {

    /**
     * Gets a game profile using Sponge's framework. This will block.
     *
     * @param uuid uuid
     * @return the game profile
     */
    public static GameProfile getProfileByUUID(UUID uuid) {
        try {
            GameProfileManager profileManager = Sponge.getServer().getGameProfileManager();
            return (GameProfile) profileManager.get(uuid).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
