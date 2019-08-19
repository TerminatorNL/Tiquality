package com.github.terminatornl.tiquality.monitor;

import com.github.terminatornl.tiquality.TiqualityConfig;
import com.github.terminatornl.tiquality.interfaces.TiqualityChunk;
import com.github.terminatornl.tiquality.util.PersistentData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;

public class ChunkLoadMonitor {

    public static final ChunkLoadMonitor INSTANCE = new ChunkLoadMonitor();
    public static final String TIQUALITY_VERSION_TAG = "Tiquality2" + (TiqualityConfig.SAVE_VERSION == 0 ? "" : TiqualityConfig.SAVE_VERSION);

    private ChunkLoadMonitor() {

    }


    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        ((TiqualityChunk) event.getChunk()).associateTrackers();
    }

    @SubscribeEvent
    public void onNBTLoad(ChunkDataEvent.Load event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        if (world.isRemote) {
            return;
        }
        PersistentData.ensureDataAvailability(world);
        NBTTagCompound tag = PersistentData.getChunkNBTData(chunk);
        if (tag != null && tag.hasKey(TIQUALITY_VERSION_TAG)) {
            NBTTagCompound tiqualityData = tag.getCompoundTag(TIQUALITY_VERSION_TAG);
            ((TiqualityChunk) event.getChunk()).tiquality_loadNBT(event.getWorld(), tiqualityData);
        }
    }

    @SubscribeEvent
    public void onNBTSave(ChunkDataEvent.Save event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        if (world.isRemote) {
            return;
        }
        NBTTagCompound tiqualityData = ((TiqualityChunk) event.getChunk()).tiquality_getNBT();
        if (tiqualityData != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(TIQUALITY_VERSION_TAG, tiqualityData);
            PersistentData.ensureDataAvailability(world);
            try {
                PersistentData.saveChunkNBTData(chunk, tag);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                PersistentData.saveChunkNBTData(chunk, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
