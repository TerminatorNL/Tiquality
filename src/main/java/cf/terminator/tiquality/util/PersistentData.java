package cf.terminator.tiquality.util;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;

/**
 * Used to tracking and access persistent data quickly using enums as keys.
 */
public enum PersistentData {
    NEXT_FREE_TRACKER_ID;

    /*
            I want to change this to the world folder someday, to make it reset on new world creation
     */
    private static final File persistentFile = new File(Loader.instance().getConfigDir(),"TiqualityStorage.nbt");
    private static final NBTTagCompound storage;

    static {
        try {
            NBTTagCompound read_tag = CompressedStreamTools.read(persistentFile);
            storage = read_tag == null ? new NBTTagCompound() : read_tag;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read persistent data file.", e);
        }
    }

    public static void save(){
        try {
            CompressedStreamTools.write(storage, persistentFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write persistent data file.", e);
        }
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




}
