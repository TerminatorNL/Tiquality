package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.DenyTracker;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;

public class TrackingTool {

    public static final TextFormatting[] COLORSCALE = new TextFormatting[]{
            TextFormatting.GRAY,
            TextFormatting.WHITE
    };

    private final EntityPlayerMP player;
    private long endTime = 0L;
    private BlockPos selectedPos;
    private int step = 0;
    private long lastTime = 0;
    private int timeout = 0;

    public TrackingTool(@Nonnull EntityPlayerMP player){
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
        Utils.sendStatusBarMessage(player,new TextComponentString("Tracking tool stopped."));
    }

    private void sendTime(){
        Utils.sendStatusBarMessage(player,new TextComponentString("Tracking tool started, aim at a block and sneak to track it. Time left: " + (((endTime - System.currentTimeMillis())/1000)+1) + "s"));
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

        if(result.getBlockPos().equals(selectedPos)){
            ++step;
        }else{
            step = 0;
        }
        selectedPos = result.getBlockPos();
        Tracker tracker = ((TiqualityWorld) player.world).getTiqualityTracker(selectedPos);

        if(tracker != null && tracker != DenyTracker.INSTANCE){
            Utils.sendStatusBarMessage(player,new TextComponentString(TextFormatting.GRAY + tracker.getInfo().getText()));
            return;
        }
        if(step == COLORSCALE.length){
            --step;
            ((TiqualityWorld) player.world).setTiqualityTracker(selectedPos, PlayerTracker.getOrCreatePlayerTrackerByProfile(((TiqualityWorld) player.world), player.getGameProfile()));
            return;
        }
        Block block = player.world.getBlockState(selectedPos).getBlock();
        String text = Block.REGISTRY.getNameForObject(block).toString();
        String dots = new String(new char[step]).replace("\0", ".");

        Utils.sendStatusBarMessage(player,new TextComponentString(COLORSCALE[step] + text + dots));
    }
}
