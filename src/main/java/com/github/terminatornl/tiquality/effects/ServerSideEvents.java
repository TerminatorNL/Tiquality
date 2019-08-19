package com.github.terminatornl.tiquality.effects;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;

public class ServerSideEvents {

    public static void spawnParticle(EntityPlayerMP player, EnumParticleTypes type, BlockPos pos, int count, float spread) {
        SPacketParticles packet = new SPacketParticles(type, false, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, spread, spread, spread, 0, count);
        player.connection.sendPacket(packet);
    }

}
