package cf.terminator.tiquality.monitor;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChunkLoadMonitor {

    public static final ChunkLoadMonitor INSTANCE = new ChunkLoadMonitor();
    public static final String TIQUALITY_TAG = "Tiquality1" + (TiqualityConfig.SAVE_VERSION == 0 ? "" : TiqualityConfig.SAVE_VERSION);

    private ChunkLoadMonitor(){

    }



    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event){
        ((TiqualityChunk) event.getChunk()).associateTrackers();
    }

    @SubscribeEvent
    public void onNBTLoad(ChunkDataEvent.Load event){
        NBTTagCompound tag = event.getData();
        if(tag.hasKey(TIQUALITY_TAG)){
            NBTTagCompound tiqualityData = tag.getCompoundTag(TIQUALITY_TAG);
            ((TiqualityChunk) event.getChunk()).tiquality_loadNBT(event.getWorld(), tiqualityData);
        }
    }

    @SubscribeEvent
    public void onNBTSave(ChunkDataEvent.Save event){
        NBTTagCompound tag = event.getData();
        NBTTagCompound tiqualityData = ((TiqualityChunk) event.getChunk()).tiquality_getNBT();
        if(tiqualityData != null) {
            tag.setTag(TIQUALITY_TAG, tiqualityData);
        }
    }
}
