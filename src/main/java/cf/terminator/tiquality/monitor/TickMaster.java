package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.Constants;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;

public class TickMaster {

    private final MinecraftServer server;
    private long startTime = 0L;
    public static long TICK_DURATION = Constants.NS_IN_TICK_LONG; /* Is updated when reloading config. */

    public TickMaster(MinecraftServer server) {
        this.server = server;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onServerTick(TickEvent.ServerTickEvent e){
        GameProfile[] cache = server.getOnlinePlayerProfiles();
        if(e.phase == TickEvent.Phase.START) {

            startTime = System.nanoTime();


            /* First, we asses the amount of active PlayerTrackers. */
            double totalWeight = 0;
            for(Map.Entry<UUID, PlayerTracker> entry : TrackerHub.getEntrySet()){
                PlayerTracker tracker = entry.getValue();
                if(tracker.isConsumer() == false){
                    continue;
                }
                totalWeight += tracker.getMultiplier(cache);
            }

            /*
                We divide the tick time amongst users, based on whether they are online or not and config multiplier.
                Source for formula: https://math.stackexchange.com/questions/253392/weighted-division
            */
            double totalweight = Math.max(1, totalWeight);

            for(Map.Entry<UUID, PlayerTracker> entry : TrackerHub.getEntrySet()){
                PlayerTracker tracker = entry.getValue();
                if(tracker.isConsumer() == false){
                    tracker.setNextTickTime(0);
                }else{
                    long time = Math.round(TICK_DURATION * (tracker.getMultiplier(cache)/totalweight));

                    //Tiquality.LOGGER.info("GRANTED: " + time + " ns. (" + ((double) time/(double) TICK_DURATION*100d) + "%) -> " + tracker.toString());
                    tracker.setNextTickTime(time);
                }
            }
        }else if(e.phase == TickEvent.Phase.END){
            TrackerHub.removeInactiveTrackers();
            TrackerHub.tickUntil(startTime + TICK_DURATION);
        }
    }
}
