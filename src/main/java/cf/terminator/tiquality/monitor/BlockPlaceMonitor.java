package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.store.BlockTracker;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

public class BlockPlaceMonitor {

    public static final BlockPlaceMonitor INSTANCE = new BlockPlaceMonitor();

    private BlockPlaceMonitor(){}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockEvent.PlaceEvent e){
        UUID uuid = e.getPlayer().getGameProfile().getId();
        if(uuid != null) {
            BlockTracker.setOwner(e.getWorld(), e.getPos(), uuid);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMultiBlockPlaceEvent(BlockEvent.MultiPlaceEvent e){
        UUID uuid = e.getPlayer().getGameProfile().getId();
        if(uuid != null) {
            for (BlockSnapshot snapshot : e.getReplacedBlockSnapshots()) {
                BlockTracker.setOwner(snapshot.getWorld(), snapshot.getPos(), uuid);
            }
        }
    }

}
