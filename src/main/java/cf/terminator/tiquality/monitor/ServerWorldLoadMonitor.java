package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.util.PersistentData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Updates the persistent file for Single player and initial setup for the dedicated server.
 */
public class ServerWorldLoadMonitor {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLoad(ChunkEvent.Load e){
        PersistentData.updatePersistentFileAndStorage(e.getWorld());
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
