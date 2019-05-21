package cf.terminator.tiquality.integration.ftbutilities;

import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.PlayerTracker;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbutilities.data.ClaimedChunk;
import com.feed_the_beast.ftbutilities.data.ClaimedChunks;
import com.feed_the_beast.ftbutilities.events.chunks.ChunkModifiedEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventHandler {

    public static final EventHandler INSTANCE = new EventHandler();

    private EventHandler(){

    }

    /*
    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        if(ClaimedChunks.isActive() == false){
            return;
        }
        ChunkDimPos pos = new ChunkDimPos(e.getEntity());
        ForgeTeam team = ClaimedChunks.instance.getChunkTeam(pos);
        if(team == null){
            return;
        }
        ((TiqualityEntity) e.getEntity()).setTracker(FTBUtilitiesHook.getTrackerForTeam((TiqualityWorld) e.getWorld(), team));
    }*/

    /*
    @SubscribeEvent
    public void onEntityEnterChunkEvent(EntityEvent.EnteringChunk e){
        if(ClaimedChunks.isActive() == false){
            return;
        }
        ChunkDimPos pos = new ChunkDimPos(e.getEntity());
        ForgeTeam team = ClaimedChunks.instance.getChunkTeam(pos);
        if(team == null){
            return;
        }
        TiqualityEntity entity = (TiqualityEntity) e.getEntity();
        if(entity.getTrackerHolder() == null){
            PlayerTracker newTracker = FTBUtilitiesHook.getTrackerForTeam((TiqualityWorld) entity.tiquality_getWorld(),team);
            if(newTracker != null){
                entity.setTracker(newTracker);
            }
        }
    }*/

    @SubscribeEvent
    public void onClaim(ChunkModifiedEvent.Claimed e){
        ClaimedChunk chunk = e.getChunk();
        ChunkDimPos dimPos = chunk.getPos();
        World world = DimensionManager.getWorld(dimPos.dim);
        if(world == null){
            return;
        }
        PlayerTracker tracker = FTBUtilitiesHook.getTrackerForTeam((TiqualityWorld) world,e.getChunk().getTeam());
        if(tracker == null){
            return;
        }
        ChunkPos pos = new ChunkPos(dimPos.posX, dimPos.posZ);
        ((TiqualityWorld) world).setTiqualityTrackerCuboidAsync(
                new BlockPos(pos.getXStart(), 0, pos.getZStart()),
                new BlockPos(pos.getXEnd(), 255, pos.getZEnd()),
                tracker,
                null
        );
    }


    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e){
        if(ClaimedChunks.isActive() == false){
            return;
        }
        ChunkPos pos = e.getChunk().getMinecraftChunk().getPos();
        ForgeTeam team = ClaimedChunks.instance.getChunkTeam(new ChunkDimPos(pos.x, pos.z, e.getChunk().getMinecraftChunk().getWorld().provider.getDimension()));
        if(team == null){
            return;
        }
        PlayerTracker tracker = FTBUtilitiesHook.getTrackerForTeam(e.getTiqualityWorld(), team);
        if(tracker != null) {
            e.setTracker(tracker);
        }
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetChunkTrackerEvent e){
        if(ClaimedChunks.isActive() == false){
            return;
        }
        ChunkPos pos = e.getChunk().getMinecraftChunk().getPos();
        ForgeTeam team = ClaimedChunks.instance.getChunkTeam(new ChunkDimPos(pos.x, pos.z, e.getChunk().getMinecraftChunk().getWorld().provider.getDimension()));
        if(team == null){
            return;
        }
        PlayerTracker tracker = FTBUtilitiesHook.getTrackerForTeam(e.getTiqualityWorld(), team);
        if(tracker != null) {
            e.setTracker(tracker);
        }
    }
}
