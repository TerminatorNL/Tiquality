package cf.terminator.tiquality.util;

import cf.terminator.tiquality.Tiquality;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.io.File;
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

    public static void updatePersistentFileAndStorage(World world){
        try {
            persistentFile = new File(world.getSaveHandler().getWorldDirectory(), "TiqualityStorage.nbt");
            Tiquality.LOGGER.info("Persistent data is inside: " + persistentFile.getCanonicalPath());
            NBTTagCompound read_tag = CompressedStreamTools.read(persistentFile);
            storage = read_tag == null ? new NBTTagCompound() : read_tag;
            save();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read persistent data file.", e);
        }
    }

    public static void save(){
        try {
            if(persistentFile.exists() == false){
                if(persistentFile.getParentFile().exists() == false && persistentFile.getParentFile().mkdirs() == false){
                    throw new IOException("Failed to create directories for: " + persistentFile);
                }
                if(persistentFile.createNewFile() == false){
                    throw new IOException("Failed to create file at: " + persistentFile);
                }
            }
            CompressedStreamTools.write(storage, persistentFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write persistent data file.", e);
        }
    }

    public void lock(){
        LOCK.lock();
    }

    public void unlock(){
        LOCK.unlock();
    }

    public boolean isSet(){
        return storage.hasKey(name());
    }

    public void setLong(long l){
        storage.setLong(name(), l);
        save();
    }

    public long getLong(){
        return storage.getLong(name());
    }

    public void setCompoundTag(NBTTagCompound tag){
        storage.setTag(name(), tag);
        save();
    }

    public NBTTagCompound getCompoundTag(){
        return storage.getCompoundTag(name());
    }

}