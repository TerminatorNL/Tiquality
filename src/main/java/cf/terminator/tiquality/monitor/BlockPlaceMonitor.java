package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.util.ForgeData;
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
            Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) e.getWorld(), ForgeData.getGameProfileByUUID(uuid));
            ((TiqualityWorld) e.getWorld()).setTiqualityTracker(e.getPos(), tracker);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMultiBlockPlaceEvent(BlockEvent.MultiPlaceEvent e){
        UUID uuid = e.getPlayer().getGameProfile().getId();
        if(uuid != null) {
            TiqualityWorld world = (TiqualityWorld) e.getWorld();
            for (BlockSnapshot snapshot : e.getReplacedBlockSnapshots()) {
                Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile(world, ForgeData.getGameProfileByUUID(uuid));
                ((TiqualityWorld) e.getWorld()).setTiqualityTracker(snapshot.getPos(), tracker);
            }
        }
    }

}
