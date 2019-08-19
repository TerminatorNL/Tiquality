package com.github.terminatornl.tiquality.monitor;

import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.tracking.PlayerTracker;
import com.github.terminatornl.tiquality.util.ForgeData;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

public class BlockPlaceMonitor {

    public static final BlockPlaceMonitor INSTANCE = new BlockPlaceMonitor();

    private BlockPlaceMonitor() {
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockEvent.PlaceEvent e) {
        UUID uuid = e.getPlayer().getGameProfile().getId();
        if (uuid != null) {
            Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) e.getWorld(), ForgeData.getGameProfileByUUID(uuid));
            ((TiqualityWorld) e.getWorld()).setTiqualityTracker(e.getPos(), tracker);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMultiBlockPlaceEvent(BlockEvent.MultiPlaceEvent e) {
        UUID uuid = e.getPlayer().getGameProfile().getId();
        if (uuid != null) {
            TiqualityWorld world = (TiqualityWorld) e.getWorld();
            for (BlockSnapshot snapshot : e.getReplacedBlockSnapshots()) {
                Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile(world, ForgeData.getGameProfileByUUID(uuid));
                ((TiqualityWorld) e.getWorld()).setTiqualityTracker(snapshot.getPos(), tracker);
            }
        }
    }

}
