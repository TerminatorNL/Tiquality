package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.Constants;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;

import static cf.terminator.tiquality.TiqualityConfig.TIME_BETWEEN_TICKS_IN_NS;

public class TickMaster {

    public static final TickMaster INSTANCE = new TickMaster();

    private final MinecraftServer server;
    private long startTime = 0L;
    private long TICK_DURATION = Constants.NS_IN_TICK_LONG - TIME_BETWEEN_TICKS_IN_NS;

    private TickMaster() {
        if(FMLCommonHandler.instance().getSide() == Side.SERVER) {
            /* We're on a server. */
            Tiquality.LOGGER.info("We're on a dedicated server.");
            server = FMLCommonHandler.instance().getMinecraftServerInstance();
        }else{
            /* We're on the client. */
            Tiquality.LOGGER.info("We're on a client.");
            server = FMLClientHandler.instance().getServer();
        }
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
