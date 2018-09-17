package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.util.Utils;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;

public class InfoMonitor {

    private final EntityPlayerMP player;
    private long endTime = 0L;
    private long lastTime = 0;
    private int timeout = 0;

    public InfoMonitor(@Nonnull EntityPlayerMP player){
        this.player = player;
    }

    /**
     * Starts this listener
     * @param time_in_ms the max time in ms for how long this monitor should be active after the last update.
     *                   Note: This time is regardless of the amount of ticks (It is real-world time)
     */
    public void start(int time_in_ms){
        timeout = time_in_ms;
        endTime = System.currentTimeMillis() + timeout;
        if(player.hasDisconnected()){
            return;
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Stops this monitor, returning a message to the player.
     */
    public void stop(){
        MinecraftForge.EVENT_BUS.unregister(this);
        if(player.hasDisconnected()){
            return;
        }
        Utils.sendStatusBarMessage(player,new TextComponentString("Info tool stopped."));
    }

    private void sendTime(){
        Utils.sendStatusBarMessage(player,new TextComponentString("Info tool started, aim at a block and sneak. Time left: " + (((endTime - System.currentTimeMillis())/1000)+1) + "s"));
    }

    @SubscribeEvent
    public void onUpdate(TickEvent.ServerTickEvent e){
        if(System.currentTimeMillis() > endTime){
            stop();
            return;
        }
        if(player.hasDisconnected()){
            stop();
            return;
        }
        if(player.isSneaking() == false){
            sendTime();
            return;
        }
        if(System.currentTimeMillis() - 100 < lastTime){
            return;
        }else{
            lastTime = System.currentTimeMillis();
        }
        RayTraceResult result = player.world.rayTraceBlocks(player.getPositionEyes(1F), Utils.getLookVec(player,25));
        if(result == null){
            Utils.sendStatusBarMessage(player,new TextComponentString("No block found."));
            return;
        }
        endTime = System.currentTimeMillis() + timeout;

        PlayerTracker tracker = ((TiqualityWorld) player.world).getPlayerTracker(result.getBlockPos());
        Block block = player.world.getBlockState(result.getBlockPos()).getBlock();

        String type = TextFormatting.WHITE.toString() + Block.REGISTRY.getNameForObject(block).toString();

        if(tracker != null){
            GameProfile owner = tracker.getOwner();
            String name = owner.getName() == null ? owner.getId().toString() : owner.getName();
            if(player.getName().equals(owner.getName())){
                Utils.sendStatusBarMessage(player,new TextComponentString(type + TextFormatting.GREEN + " Claimed by you."));
            }else{
                Utils.sendStatusBarMessage(player,new TextComponentString(type + TextFormatting.RED + " Claimed by: " + name));
            }
            return;
        }

        Utils.sendStatusBarMessage(player,new TextComponentString(type + TextFormatting.AQUA + " Unclaimed."));
    }
}
