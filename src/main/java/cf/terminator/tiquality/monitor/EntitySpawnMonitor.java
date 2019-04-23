package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.tracking.UpdateType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntitySpawnMonitor {

    public static final EntitySpawnMonitor INSTANCE = new EntitySpawnMonitor();

    private EntitySpawnMonitor(){

    }

    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        ResourceLocation location = ((TiqualityEntity) e.getEntity()).tiquality_getResourceLocation();
        UpdateType type = TiqualityConfig.QuickConfig.ENTITY_UPDATE_TYPES.get(location);
        ((TiqualityEntity) e.getEntity()).setUpdateType((type == null) ? UpdateType.DEFAULT : type);
        if(e.getEntity() instanceof EntityPlayer){
            ((TiqualityEntity) e.getEntity()).setUpdateType(UpdateType.ALWAYS_TICK);
        }
    }
}
