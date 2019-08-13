package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.UpdateType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntityMonitor {

    public static final EntityMonitor INSTANCE = new EntityMonitor();

    private EntityMonitor(){

    }

    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        if (e.getWorld().isRemote) {
            ((TiqualityEntity) e.getEntity()).setUpdateType(UpdateType.ALWAYS_TICK);
        } else {
            ResourceLocation location = ((TiqualityEntity) e.getEntity()).tiquality_getResourceLocation();
            ((TiqualityEntity) e.getEntity()).setUpdateType(TiqualityConfig.QuickConfig.getEntityUpdateType(location));
            if (e.getEntity() instanceof EntityPlayer) {
                ((TiqualityEntity) e.getEntity()).setUpdateType(UpdateType.ALWAYS_TICK);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeChunk(EntityEvent.EnteringChunk event){
        TiqualityEntity entity = (TiqualityEntity) event.getEntity();
        Tracker tracker = entity.getTracker();
        if(tracker == null){
            BlockPos pos = entity.tiquality_getPos();
            if(entity.tiquality_getWorld().isBlockLoaded(pos)){
                TiqualityChunk chunk = (TiqualityChunk) entity.tiquality_getWorld().getChunk(pos);
                Tracker dominantTracker = chunk.getCachedMostDominantTracker();
                if(dominantTracker != null){
                    entity.setTracker(dominantTracker);
                }
            }
        }
    }
}
