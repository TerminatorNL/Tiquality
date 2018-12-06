package cf.terminator.tiquality.util;

import cf.terminator.tiquality.Tiquality;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to tracking and access persistent data quickly using enums as keys.
 */
public enum PersistentData {
    NEXT_FREE_TRACKER_ID,
    TRACKER_TO_ID;




    private static final File persistentFile;
    private static final NBTTagCompound storage;

    static {
        try {
            persistentFile = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getFile(extractLevelName()), "TiqualityStorage.nbt");
            Tiquality.LOGGER.info("Persistent data is inside: " + persistentFile.getCanonicalPath());
            NBTTagCompound read_tag = CompressedStreamTools.read(persistentFile);
            storage = read_tag == null ? new NBTTagCompound() : read_tag;
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

    private static String extractLevelName(){
        try {
            File serverProperties = FMLCommonHandler.instance().getMinecraftServerInstance().getFile("server.properties");
            BufferedReader reader = new BufferedReader(new FileReader(serverProperties));
            while(true){
                String line = reader.readLine();
                if(line == null){
                    break;
                }
                if(line.toLowerCase().startsWith("level-name=")){
                    Pattern pattern = Pattern.compile("level-name=(.+)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(line);
                    if(matcher.find() == false){
                        throw new RuntimeException("Unable to parse the 'level-name' in server.properties! Attempted to parse: '" + line + "' -> " + Arrays.toString(line.getBytes()));
                    }
                    return matcher.group(1);
                }
            }
            throw new RuntimeException("Unable to find the 'level-name' in server.properties!");
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

}
