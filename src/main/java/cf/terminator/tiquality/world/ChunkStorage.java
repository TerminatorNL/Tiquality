package cf.terminator.tiquality.world;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ChunkStorage {

    private Element[] data = new Element[16];

    public ChunkStorage(){
    }

    /**
     * Removes all data from this ChunkStorage.
     */
    public void clearAll(){
        data = new Element[16];
    }

    /**
     * Sets all data from this ChunkStorage.
     */
    public void setAll(byte owner_id){
        data = new Element[16];

        byte[] storage = new byte[4096];
        Arrays.fill(storage, owner_id);

        for(int i=0;i<data.length;i++){
            data[i] = new Element(storage);
        }
    }

    /**
     * Gets the stored owner associated with the BlockPos.
     * @param pos the block pos
     * @return the owner ID, or 0 if none is found
     */
    public byte get(BlockPos pos){
        int y_layer =  pos.getY() >> 4;
        return data[y_layer] == null ? 0 : data[y_layer].get(pos);
    }

    /**
     * Sets the stored owner associated with the BlockPos.
     * @param pos the block pos
     * @param owner_id the owner ID
     */
    public void set(BlockPos pos, byte owner_id){
        int y_layer =  pos.getY() >> 4;
        if(data[y_layer] == null) {
            data[y_layer] = new Element();
        }
        data[y_layer].set(pos, owner_id);
    }

    public ArrayList<byte[]> getAll(){
        ArrayList<byte[]> list = new ArrayList<>();
        for (Element e : data) {
            if (e != null) {
                if (e.hasData()) {
                    list.add(e.storage);
                }
            }
        }
        return list;
    }

    public void loadFromNBT(NBTTagList sections){
        Iterator<NBTBase> iterator = sections.iterator();
        while(iterator.hasNext()){
            NBTTagCompound tag = (NBTTagCompound) iterator.next();
            if(tag.hasKey("Tiquality")){
                byte y_level = tag.getByte("Y");
                byte[] storage = tag.getByteArray("Tiquality");
                data[y_level] = new Element(storage);
            }
        }
    }

    public void injectNBTAfter(NBTTagList sections){
        for(int i=0;i<data.length;++i){
            Element e = data[i];
            if(e != null){
                if(e.hasData() == false){
                    continue;
                }
                if(i >= sections.tagCount()){
                    continue;
                }
                NBTTagCompound injectable = sections.getCompoundTagAt(i);
                injectable.setByteArray("Tiquality",e.storage);
                sections.set(i, injectable);
            }
        }
    }

    public static class Element{

        private final byte[] storage;

        Element(){
            storage = new byte[4096];
        }

        /**
         * Creates a new storage element with the given array.
         * THIS MUST HAVE A LENGTH OF 4096.
         * @param storage byte array
         */
        Element(byte[] storage){
            this.storage = storage;
        }

        boolean hasData(){
            for(byte b : storage){
                if(b != 0){
                    return true;
                }
            }
            return false;
        }

        int getIndex(BlockPos pos){
            return (pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | (pos.getX() & 15);
        }

        /**
         * Gets the stored owner associated with the BlockPos.
         * @param pos the block pos
         * @return the owner ID, or 0 if none is found
         */
        byte get(BlockPos pos){
            return storage[getIndex(pos)];
        }

        /**
         * Sets the stored owner associated with the BlockPos.
         * @param pos the block pos
         */
        void set(BlockPos pos, byte owner_id){
            storage[getIndex(pos)] = owner_id;
        }


    }
}
