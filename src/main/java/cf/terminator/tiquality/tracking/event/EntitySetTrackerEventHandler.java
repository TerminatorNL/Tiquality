package cf.terminator.tiquality.tracking.event;

import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.tracking.ForcedTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntitySetTrackerEventHandler {

    public static final EntitySetTrackerEventHandler INSTANCE = new EntitySetTrackerEventHandler();

    private EntitySetTrackerEventHandler(){

    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onSet(TiqualityEvent.SetEntityTrackerEvent e){
        TiqualityEntity entity = e.getEntity();
        if(entity instanceof EntityPlayer){
            e.setHolder(ForcedTracker.INSTANCE.getHolder());
        }
    }
}
