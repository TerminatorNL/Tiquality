package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.Constants;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;

import static cf.terminator.tiquality.TiqualityConfig.TIME_BETWEEN_TICKS_IN_NS;
import static cf.terminator.tiquality.store.TrackerHub.UNSAFE_SERVER_THREAD_ONLY_TRACKER;

public class TickMaster {

    public static final TickMaster INSTANCE = new TickMaster();

    private final MinecraftServer server;
    private long startTime = 0L;
    private long TICK_DURATION = Constants.NS_IN_TICK_LONG - TIME_BETWEEN_TICKS_IN_NS;

    private TickMaster() {
        this.server = FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(TickEvent.ServerTickEvent e){
        GameProfile[] cache = server.getOnlinePlayerProfiles();
        if(e.phase == TickEvent.Phase.START) {

            startTime = System.nanoTime();


            /* First, we asses the amount of active PlayerTrackers. */
            double totalWeight = 0;
            for(Map.Entry<UUID, PlayerTracker> entry : UNSAFE_SERVER_THREAD_ONLY_TRACKER.entrySet()){
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

            for(Map.Entry<UUID, PlayerTracker> entry : UNSAFE_SERVER_THREAD_ONLY_TRACKER.entrySet()){
                PlayerTracker tracker = entry.getValue();
                if(tracker.isConsumer() == false){
                    tracker.setNextTickTime(0);
                }else{
                    tracker.setNextTickTime(Math.round(TICK_DURATION * (tracker.getMultiplier(cache)/totalweight)));
                }
            }
        }else if(e.phase == TickEvent.Phase.END){
            TrackerHub.removeInactiveTrackers();
            TrackerHub.tickUntil(startTime + TICK_DURATION);
        }
    }
}
