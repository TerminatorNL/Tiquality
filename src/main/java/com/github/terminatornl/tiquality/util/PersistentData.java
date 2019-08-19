package com.github.terminatornl.tiquality.util;

import com.github.terminatornl.tiquality.Tiquality;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to tracking and access persistent data quickly using enums as keys.
 */
public enum PersistentData {
    NEXT_FREE_TRACKER_ID,
    ID_TO_TRACKER,
    TRACKER_LOOKUP;

    private ReentrantLock LOCK = new ReentrantLock();

    private static File persistentFile;
    private static NBTTagCompound storage;
    private static File worldFolder;
    private static File tiqualityFolder;
    private static File tiqualityWorldData;

    public static void updatePersistentFileAndStorage(World world) {
        try {
            File worldFolderTmp = world.getSaveHandler().getWorldDirectory();
            if (worldFolderTmp.equals(worldFolder)) {
                return;
            }
            worldFolder = worldFolderTmp;
            tiqualityFolder = new File(worldFolder, "TiqualityStorage");
            persistentFile = new File(tiqualityFolder, "Storage");
            tiqualityWorldData = new File(tiqualityFolder, "WorldData");
            if (tiqualityFolder.exists() == false && tiqualityFolder.mkdirs() == false) {
                throw new RuntimeException(new IOException("Unable to create directory at: " + tiqualityFolder));
            }
            Tiquality.LOGGER.info("Persistent data is inside: " + tiqualityFolder.getCanonicalPath());
            NBTTagCompound read_tag = CompressedStreamTools.read(persistentFile);
            storage = read_tag == null ? new NBTTagCompound() : read_tag;
            save();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read persistent data file.", e);
        }
    }

    @Nonnull
    public static File getChunkFile(Chunk chunk) {
        int dim = chunk.getWorld().provider.getDimension();
        String pos = chunk.x + "_" + chunk.z + ".nbt";
        File worldFile = new File(tiqualityWorldData, String.valueOf(dim));
        return new File(worldFile, pos);
    }

    @Nonnull
    public static File getTempChunkFile(Chunk chunk) {
        int dim = chunk.getWorld().provider.getDimension();
        String pos = chunk.x + "_" + chunk.z + ".tmp.nbt";
        File worldFile = new File(tiqualityWorldData, String.valueOf(dim));
        return new File(worldFile, pos);
    }

    @Nullable
    public static NBTTagCompound getChunkNBTData(Chunk chunk) {
        File chunkFile = getChunkFile(chunk);
        if (chunkFile.exists() == false) {
            return null;
        } else {
            try {
                return CompressedStreamTools.readCompressed(new FileInputStream(chunkFile));
            } catch (IOException e) {
                Tiquality.LOGGER.fatal("FAILED TO LOAD DATA FOR CHUNK AT " + chunk.getPos());
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void saveChunkNBTData(Chunk chunk, @Nullable NBTTagCompound tag) throws IOException {
        File tempFile = getTempChunkFile(chunk);
        File chunkFile = getChunkFile(chunk);
        if (tag == null) {
            if (chunkFile.exists() && chunkFile.delete() == false) {
                throw new IOException("Unable to remove file: " + chunkFile);
            }
            return;
        }
        if (tempFile.exists() == false) {
            if (tempFile.getParentFile().exists() == false && tempFile.getParentFile().mkdirs() == false) {
                throw new IOException("Unable to create directories at: " + tempFile.getParentFile());
            }
            try {
                if (tempFile.createNewFile() == false) {
                    throw new IOException("Unable to create file at: " + tempFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        CompressedStreamTools.writeCompressed(tag, new FileOutputStream(tempFile));
        if (chunkFile.exists() && chunkFile.delete() == false) {
            throw new IOException("Failed to remove old file: " + chunkFile);
        }
        if (tempFile.renameTo(chunkFile) == false) {
            throw new IOException("Failed to rename file " + tempFile + " to " + chunkFile);
        }
    }

    public synchronized static void ensureDataAvailability(World world) {
        if (isAvailable() == false) {
            updatePersistentFileAndStorage(world);
        }
    }

    public static boolean isAvailable() {
        return storage != null;
    }

    @SuppressWarnings("AssignmentToNull")
    public static void deactivate() {
        storage = null;
        persistentFile = null;
        worldFolder = null;
        tiqualityFolder = null;
    }

    public static void save() {
        try {
            if (persistentFile.exists() == false) {
                if (persistentFile.getParentFile().exists() == false && persistentFile.getParentFile().mkdirs() == false) {
                    throw new IOException("Failed to create directories for: " + persistentFile);
                }
                if (persistentFile.createNewFile() == false) {
                    throw new IOException("Failed to create file at: " + persistentFile);
                }
            }
            CompressedStreamTools.write(storage, persistentFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write persistent data file.", e);
        }
    }

    public void lock() {
        LOCK.lock();
    }

    public void unlock() {
        LOCK.unlock();
    }

    public boolean isSet() {
        return storage.hasKey(name());
    }

    public long getLong() {
        return storage.getLong(name());
    }

    public void setLong(long l) {
        storage.setLong(name(), l);
        save();
    }

    public NBTTagCompound getCompoundTag() {
        return storage.getCompoundTag(name());
    }

    public void setCompoundTag(NBTTagCompound tag) {
        storage.setTag(name(), tag);
        save();
    }

}