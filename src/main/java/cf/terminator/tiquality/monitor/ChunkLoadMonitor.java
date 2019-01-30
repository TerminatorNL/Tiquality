package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChunkLoadMonitor {

    public static final ChunkLoadMonitor INSTANCE = new ChunkLoadMonitor();

    private ChunkLoadMonitor(){

    }



    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event){
        ((TiqualityChunk) event.getChunk()).associateTrackers();
    }

    @SubscribeEvent
    public void onNBTLoad(ChunkDataEvent.Load event){
        NBTTagCompound tag = event.getData();
        NBTTagCompound level = tag.getCompoundTag("Level");
        ((TiqualityChunk) event.getChunk()).tiquality_loadNBT(event.getWorld(), level);
    }

    @SubscribeEvent
    public void onNBTSave(ChunkDataEvent.Save event){
        NBTTagCompound tag = event.getData();
        NBTTagCompound level = tag.getCompoundTag("Level");
        ((TiqualityChunk) event.getChunk()).tiquality_writeToNBT(level);
        tag.setTag("Level", level);
    }
}
