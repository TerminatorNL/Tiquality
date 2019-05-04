package cf.terminator.tiquality.util;

import cf.terminator.tiquality.Tiquality;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public class Teleporting {

    public static void attemptTeleportWithGameMode3(EntityPlayer player, int dimension, BlockPos pos) {
        if(player instanceof EntityPlayerMP){
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            switch (playerMP.interactionManager.getGameType()){
                case SURVIVAL:
                case NOT_SET:
                case ADVENTURE:
                    player.sendMessage(new TextComponentString(Tiquality.PREFIX + "Your game-mode was set to SPECTATOR to prevent accidental deaths."));
                    player.setGameType(GameType.SPECTATOR);
            }
        }
        if(player.getEntityWorld().provider.getDimension() != dimension){
            attemptTeleportAcrossWorlds(player, dimension, pos);
        }else{
            setPlayerPosition(player, pos);
        }
    }

    private static void attemptTeleportAcrossWorlds(EntityPlayer player, int dimension, BlockPos pos) {
        player.changeDimension(dimension, new Teleporter(pos));
    }

    private static void setPlayerPosition(EntityPlayer player, BlockPos pos){
        double xPos = ((double) pos.getX()) + 0.5D;
        double yPos = ((double) pos.getY()) + 0.5D;
        double zPos = ((double) pos.getZ()) + 0.5D;
        player.setPosition(xPos, yPos, zPos);
        if(player instanceof EntityPlayerMP){
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            playerMP.connection.setPlayerLocation(xPos, yPos, zPos, player.rotationYaw, player.rotationPitch);
        }
    }

    private static class Teleporter implements ITeleporter{

        private final BlockPos pos;

        private Teleporter(BlockPos pos){
            this.pos = pos;
        }

        @Override
        public void placeEntity(World world, Entity entity, float yaw) {
            double xPos = ((double) pos.getX()) + 0.5D;
            double yPos = ((double) pos.getY()) + 0.5D;
            double zPos = ((double) pos.getZ()) + 0.5D;


            entity.setLocationAndAngles(xPos, yPos, zPos, entity.rotationYaw, 0.0F);
            entity.motionX = 0.0D;
            entity.motionY = 0.0D;
            entity.motionZ = 0.0D;
            if(entity instanceof EntityPlayerMP){
                EntityPlayerMP player = (EntityPlayerMP) entity;
                player.connection.setPlayerLocation(xPos, yPos, zPos, entity.rotationYaw, entity.rotationPitch);
            }
        }
    }
}
