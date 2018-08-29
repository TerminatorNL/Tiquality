package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.store.BlockTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockPlaceMonitor {

    public static final BlockPlaceMonitor INSTANCE = new BlockPlaceMonitor();

    private BlockPlaceMonitor(){}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockEvent.PlaceEvent e){
        BlockTracker.setOwner(e.getWorld(), e.getPos(), e.getPlayer().getGameProfile().getId());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMultiBlockPlaceEvent(BlockEvent.MultiPlaceEvent e){
        EntityPlayer player = e.getPlayer();
        for(BlockSnapshot snapshot: e.getReplacedBlockSnapshots()){
            BlockTracker.setOwner(snapshot.getWorld(), snapshot.getPos(), player.getGameProfile().getId());
        }
    }

}
