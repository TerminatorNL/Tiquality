package com.github.terminatornl.tiquality.tracking.event;

import com.github.terminatornl.tiquality.api.event.TiqualityEvent;
import com.github.terminatornl.tiquality.interfaces.TiqualityEntity;
import com.github.terminatornl.tiquality.tracking.ForcedTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntitySetTrackerEventHandler {

    public static final EntitySetTrackerEventHandler INSTANCE = new EntitySetTrackerEventHandler();

    private EntitySetTrackerEventHandler() {

    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onSet(TiqualityEvent.SetEntityTrackerEvent e) {
        TiqualityEntity entity = e.getEntity();
        if (entity instanceof EntityPlayer) {
            e.setHolder(ForcedTracker.INSTANCE.getHolder());
        }
    }
}
